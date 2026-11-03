package com.tuned.app.data.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
enum class PinKind { SONG, ALBUM, ARTIST }

/** Something pinned to the top of Library, with what's needed to show and open it offline. */
@Serializable
data class PinnedItem(
    val kind: PinKind,
    val title: String,
    val artist: String = "",
    val artworkUrl: String? = null,
    val browseId: String? = null,
    val videoId: String? = null,
) {
    /** Same thing however it was reached: by id when there is one, otherwise by name. */
    val key: String get() = "$kind|${videoId ?: browseId ?: "$title|$artist".lowercase()}"
}

/** Apple Music's Pins: songs, albums and artists kept at the top of Library. Newest first, on the device. */
@Singleton
class PinsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PinnedItem.serializer())

    private val _pins = MutableStateFlow(load())
    val pins: StateFlow<List<PinnedItem>> = _pins.asStateFlow()

    /**
     * One pin, one identity — however it was reached. The screens build
     * the item differently (artist page: nav-param id and no artist field;
     * album page: loaded-data id; song menu: videoId), so a strict key
     * comparison let the same artist/album exist twice and read as
     * unpinned from one path. Ids decide when both sides have one;
     * otherwise title (plus artist, when both sides name one) decides.
     */
    private fun samePin(a: PinnedItem, b: PinnedItem): Boolean {
        if (a.kind != b.kind) return false
        val aId = a.videoId ?: a.browseId
        val bId = b.videoId ?: b.browseId
        if (aId != null && bId != null) return aId == bId
        if (!a.title.equals(b.title, ignoreCase = true)) return false
        return a.artist.isBlank() || b.artist.isBlank() ||
            a.artist.equals(b.artist, ignoreCase = true)
    }

    fun isPinned(item: PinnedItem): Boolean = _pins.value.any { samePin(it, item) }

    /** Pins or unpins [item]; returns true when it is now pinned. */
    fun toggle(item: PinnedItem): Boolean {
        val pinned = isPinned(item)
        save(if (pinned) _pins.value.filterNot { samePin(it, item) } else listOf(item) + _pins.value)
        return !pinned
    }

    private fun load(): List<PinnedItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrElse {
                // A corrupt value must not read as "no pins": the next
                // toggle would overwrite the only copy with a single item.
                // Preserve the raw payload under a backup key first.
                if (prefs.getString(KEY_BACKUP, null) == null) {
                    prefs.edit().putString(KEY_BACKUP, raw).apply()
                }
                emptyList()
            }
    }

    private fun save(items: List<PinnedItem>) {
        _pins.value = items
        prefs.edit().putString(KEY, json.encodeToString(serializer, items)).apply()
    }

    private companion object {
        const val PREFS = "library_pins"
        const val KEY = "pins"
        const val KEY_BACKUP = "pins_corrupt_backup"
    }
}
