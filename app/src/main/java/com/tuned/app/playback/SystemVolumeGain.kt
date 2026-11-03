package com.tuned.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

/**
 * The media volume as a linear gain, for USB routes where Android's mixer is out of
 * the path and nothing else would apply it: the DAC would play at full scale whatever
 * the volume slider says. Cached, because the render thread reads it for every buffer.
 */
class SystemVolumeGain(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    var linear: Float = readLinear()
        private set

    init {
        runCatching {
            ContextCompat.registerReceiver(
                context.applicationContext,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        linear = readLinear()
                    }
                },
                IntentFilter(VOLUME_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    private fun readLinear(): Float {
        val audio = manager ?: return 1f
        val max = runCatching { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        if (max <= 0) return 1f
        val level = runCatching { audio.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(max)
        return UacFeatureVolume.perceptualLinearGain(level.coerceIn(0, max).toFloat() / max)
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
