package com.tuned.app.playback.analysis

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.nio.ByteOrder

// Only the part the blend will overlap matters: a beat that enters later can't clash.
private const val ANALYSIS_SECONDS = 20
private const val DEADLINE_MS = 15_000L
private const val CODEC_TIMEOUT_US = 10_000L

/**
 * Finds the beat of a song before it plays, by decoding its opening off the audio path.
 * Reads through [source], so it shares the playback cache and fills it for the real thing.
 * Returns null when the song can't be read or has no clear beat. Blocks; call off the main thread.
 */
fun analyzeSongStart(source: DataSource, uri: Uri, cacheKey: String?): TempoEstimate? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(DataSourceReader(source, uri, cacheKey))
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME) ?: return null)
        codec = decoder
        decoder.configure(format, null, null, 0)
        decoder.start()

        val values = ArrayList<Float>(ANALYSIS_SECONDS * 1000 / ONSET_HOP_MS)
        var envelope: OnsetEnvelope? = null
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        val info = MediaCodec.BufferInfo()
        val deadline = System.currentTimeMillis() + DEADLINE_MS
        var inputDone = false
        while (System.currentTimeMillis() < deadline) {
            if (!inputDone) {
                val slot = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                if (slot >= 0) {
                    val input = decoder.getInputBuffer(slot) ?: return null
                    val size = extractor.readSampleData(input, 0)
                    if (size < 0 || extractor.sampleTime > ANALYSIS_SECONDS * 1_000_000L) {
                        decoder.queueInputBuffer(slot, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(slot, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val out = decoder.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val output = decoder.outputFormat
                val rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                envelope = OnsetEnvelope(rate * channels)
                if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    encoding = output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                }
            } else if (out >= 0) {
                val pcm = decoder.getOutputBuffer(out)?.order(ByteOrder.nativeOrder())
                val onset = envelope
                if (pcm != null && onset != null) {
                    pcm.position(info.offset)
                    pcm.limit(info.offset + info.size)
                    if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                        val samples = pcm.asFloatBuffer()
                        while (samples.hasRemaining()) {
                            val value = onset.feed(samples.get().toDouble())
                            if (!value.isNaN()) values += value
                        }
                    } else {
                        val samples = pcm.asShortBuffer()
                        while (samples.hasRemaining()) {
                            val value = onset.feed(samples.get() / 32768.0)
                            if (!value.isNaN()) values += value
                        }
                    }
                }
                decoder.releaseOutputBuffer(out, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        return estimateTempo(values.toFloatArray())
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { extractor.release() }
    }
}

/** Lets MediaExtractor read through a Media3 data source. */
private class DataSourceReader(
    private val source: DataSource,
    private val uri: Uri,
    private val cacheKey: String?,
) : MediaDataSource() {
    private var openAt = -1L
    private var length = -1L

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position != openAt) {
            closeSource()
            val spec = DataSpec.Builder().setUri(uri).setPosition(position).setKey(cacheKey).build()
            val opened = source.open(spec)
            if (position == 0L && opened != C.LENGTH_UNSET.toLong()) length = opened
            openAt = position
        }
        val read = source.read(buffer, offset, size)
        if (read == C.RESULT_END_OF_INPUT) return -1
        openAt += read
        return read
    }

    override fun getSize(): Long = length

    override fun close() = closeSource()

    private fun closeSource() {
        runCatching { source.close() }
        openAt = -1L
    }
}
