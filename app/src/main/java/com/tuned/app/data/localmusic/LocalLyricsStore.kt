package com.tuned.app.data.localmusic

import android.content.Context
import java.io.File

/**
 * Sidecar `.lrc` lyrics for in-place local tracks.
 *
 * Local songs used to be copied into app storage, lyrics file beside the
 * audio file. Tracks are now played in place (SAF / MediaStore
 * `content://` references), where the player cannot derive a sibling file
 * path from the URL — so at scan time the (tiny) lyrics text is stashed
 * here, one file per track, named by the same stable id the importer
 * uses. Only lyrics text lives here; audio is never copied.
 */
object LocalLyricsStore {

    private const val DIR = "local_lyrics"
    private const val MAX_LRC_BYTES = 1024 * 1024

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun fileFor(context: Context, trackUrl: String): File =
        File(dir(context), LocalMusicNaming.stableId(trackUrl) + ".lrc")

    /** Absolute path of the stored sidecar for [trackUrl], when one exists. */
    fun pathIfExists(context: Context, trackUrl: String?): String? {
        if (trackUrl.isNullOrBlank()) return null
        val file = fileFor(context, trackUrl)
        return file.absolutePath.takeIf { file.isFile && file.length() > 0L }
    }

    /** Stores [text] as the sidecar for [trackUrl]; no-ops when unchanged. */
    fun put(context: Context, trackUrl: String, text: String) {
        if (text.isBlank()) return
        // The cap is a BYTE cap: comparing char length let multi-byte text
        // (e.g. CJK lyrics at 3 bytes/char) through at 3x the limit.
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_LRC_BYTES) return
        val file = fileFor(context, trackUrl)
        // Skip the full read when the size alone proves the text differs;
        // on a size match compare bytes, not a decoded string.
        if (file.isFile && file.length() == bytes.size.toLong() &&
            runCatching { file.readBytes().contentEquals(bytes) }.getOrDefault(false)
        ) {
            return
        }
        runCatching { file.writeBytes(bytes) }
    }

    fun remove(context: Context, trackUrl: String) {
        runCatching { fileFor(context, trackUrl).delete() }
    }

    /** Deletes sidecars whose track URL is no longer in [liveUrls]. */
    fun prune(context: Context, liveUrls: Set<String>) {
        val keep = liveUrls.mapTo(mutableSetOf()) { LocalMusicNaming.stableId(it) + ".lrc" }
        dir(context).listFiles()?.forEach { file ->
            if (file.isFile && file.name !in keep) runCatching { file.delete() }
        }
    }
}
