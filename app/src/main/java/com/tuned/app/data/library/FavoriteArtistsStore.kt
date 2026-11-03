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
data class FavoriteArtist(
    val name: String,
    val browseId: String = "",
    val artworkUrl: String? = null,
    val addedAtMillis: Long = 0L,
)

/**
 * Artists the user starred on an artist page (Apple Music's Favourite). Kept on the device,
 * newest first, keyed by name so the same artist from search and from a song menu is one entry.
 */
@Singleton
class FavoriteArtistsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(FavoriteArtist.serializer())

    private val _artists = MutableStateFlow(load())
    val artists: StateFlow<List<FavoriteArtist>> = _artists.asStateFlow()

    /**
     * Identity: the browse id when both sides carry one (two different
     * artists can share a name; a name-only key collided them), else the
     * name — the fallback for entries saved before ids were recorded.
     */
    private fun sameArtist(stored: FavoriteArtist, name: String, browseId: String): Boolean {
        if (stored.browseId.isNotBlank() && browseId.isNotBlank()) {
            return stored.browseId == browseId
        }
        return stored.name.equals(name, ignoreCase = true)
    }

    fun isFavorite(name: String): Boolean = _artists.value.any { it.name.equals(name, ignoreCase = true) }

    fun isFavorite(name: String, browseId: String): Boolean =
        _artists.value.any { sameArtist(it, name, browseId) }

    /** Adds or removes [name]; returns true when it is now a favourite. */
    fun toggle(name: String, browseId: String, artworkUrl: String?): Boolean {
        val current = _artists.value
        val next = if (current.any { sameArtist(it, name, browseId) }) {
            current.filterNot { sameArtist(it, name, browseId) }
        } else {
            listOf(FavoriteArtist(name, browseId, artworkUrl, System.currentTimeMillis())) + current
        }
        _artists.value = next
        prefs.edit().putString(KEY, json.encodeToString(serializer, next)).apply()
        return next.size > current.size
    }

    private fun load(): List<FavoriteArtist> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrElse {
                // Corrupt ≠ empty: preserve the raw payload before any
                // later toggle can overwrite the only copy.
                if (prefs.getString(KEY_BACKUP, null) == null) {
                    prefs.edit().putString(KEY_BACKUP, raw).apply()
                }
                emptyList()
            }
    }

    private companion object {
        const val PREFS = "favorite_artists"
        const val KEY = "artists"
        const val KEY_BACKUP = "artists_corrupt_backup"
    }
}
