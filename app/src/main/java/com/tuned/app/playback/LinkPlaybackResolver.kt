package com.tuned.app.playback

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tuned.app.data.music.InnerTubeMusicApi
import com.tuned.app.data.music.YouTubeMusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LinkPlaybackResolver"
/** Routes need a name; the page replaces these with the real one as soon as it loads. */
private const val LINK_ALBUM_TITLE = "Album"
private const val LINK_ARTIST_NAME = "Artist"

@Singleton
class LinkPlaybackResolver @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val innerTube: InnerTubeMusicApi,
    private val http: OkHttpClient,
    private val routeNavigator: com.tuned.app.ui.navigation.AppRouteNavigator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        val action = intent.action
        val dataUri = intent.data
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        val rawTarget = when {
            action == Intent.ACTION_VIEW && dataUri != null -> dataUri.toString()
            action == Intent.ACTION_SEND && !sharedText.isNullOrBlank() -> extractUrlOrText(sharedText)
            dataUri != null -> dataUri.toString()
            else -> null
        } ?: return false

        scope.launch {
            resolveAndPlay(rawTarget)
        }
        return true
    }

    private fun extractUrlOrText(text: String): String {
        val trimmed = text.trim()
        val urlRegex = Regex("(https?://[^\\s]+)")
        val match = urlRegex.find(trimmed)
        return match?.value ?: trimmed
    }

    suspend fun resolveAndPlay(target: String) = withContext(Dispatchers.IO) {
        val clean = target.trim()
        Log.d(TAG, "Resolving incoming target for direct playback: $clean")

        try {
            when {
                // 1. YouTube & YouTube Music URLs
                isYouTubeUrl(clean) -> resolveYouTube(clean)

                // 2. Spotify URLs
                isSpotifyUrl(clean) -> resolveSpotify(clean)

                // 3. Plain search query shared to app
                clean.isNotBlank() -> resolveGenericQuery(clean)

                else -> Unit
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Never eat cancellation — it would keep a cancelled resolve
            // "running" and mask structured shutdown.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve link for playback: $clean", e)
        }
    }

    // Host checks parse the URL instead of substring-matching it: the old
    // contains() tests accepted look-alike hosts (notyoutube.com.evil.tld)
    // and any shared text merely mentioning "youtube.com".
    private fun hostOf(url: String): String? =
        runCatching { Uri.parse(url.trim()).host?.lowercase() }.getOrNull()

    private fun isYouTubeUrl(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return host == "youtu.be" || host == "youtube.com" ||
            host.endsWith(".youtube.com")
    }

    private fun isSpotifyUrl(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return host == "spotify.link" || host == "spotify.com" ||
            host.endsWith(".spotify.com")
    }

    private suspend fun resolveYouTube(url: String) {
        when (val link = parseYouTubeLink(url)) {
            // A link to a list or an artist opens its page; nothing starts until it's chosen.
            is YouTubeLink.Album -> routeNavigator.navigateTo(
                com.tuned.app.ui.navigation.Screen.AlbumDetail.createRoute(LINK_ALBUM_TITLE, "", link.browseId),
            )
            is YouTubeLink.Playlist -> routeNavigator.navigateTo(
                com.tuned.app.ui.navigation.Screen.FeedPlaylistDetail.createRoute(link.id),
            )
            is YouTubeLink.Artist -> {
                // The page looks up photos and video by name, so it needs the real one.
                val name = runCatching { innerTube.fetchArtistPage(link.browseId) }.getOrNull()?.name
                routeNavigator.navigateTo(
                    com.tuned.app.ui.navigation.Screen.ArtistDetail.createRoute(
                        name?.takeIf(String::isNotBlank) ?: LINK_ARTIST_NAME,
                        link.browseId,
                    ),
                )
            }
            is YouTubeLink.Track -> playTrackLink(url, link)
            null -> extractYouTubeVideoId(url)?.takeIf(String::isNotBlank)?.let { playTrackLink(url, YouTubeLink.Track(it)) }
        }
    }

    private suspend fun playTrackLink(url: String, link: YouTubeLink.Track) {
        val videoId = link.videoId
        val listId = link.listId
        // A mix (RD...) is built around the song, and the player makes its own radio.
        if (listId != null && !listId.startsWith("RD")) {
            val playlist = runCatching { innerTube.fetchPlaylist(listId) }.getOrNull()
            val tracks = playlist?.tracks.orEmpty()
            val start = tracks.indexOfFirst { it.videoId == videoId }
            if (playlist != null && start >= 0) {
                withContext(Dispatchers.Main) {
                    musicPlayer.playQueue(tracks.map { it.toPlayable() }, startIndex = start, sourceLabel = playlist.title)
                }
                return
            }
        }
        val songDetails = innerTube.fetchSongDetails(videoId)
        val playable = if (songDetails != null) {
            songDetails.toPlayable()
        } else {
            val tracks = innerTube.searchSongs(videoId, limit = 1)
            val matched = tracks.firstOrNull { it.videoId == videoId }
                ?: innerTube.searchSongs(url, limit = 1).firstOrNull()
            matched?.toPlayable() ?: PlayableTrack(
                title = "YouTube Stream",
                artist = "YouTube Music",
                artworkUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                videoId = videoId,
            )
        }
        withContext(Dispatchers.Main) {
            musicPlayer.play(playable, sourceLabel = "YouTube Music", startRadio = true)
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        // Structured fallback (the primary parser is parseYouTubeLink): the
        // old chain substring-matched "v=" anywhere in the string, so a
        // parameter like "prev=xyz" or an unrelated host could yield a bogus
        // "video id". Parse host, path segments and the real query instead.
        val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val segments = uri.pathSegments.orEmpty()
        return when {
            host == "youtu.be" -> segments.firstOrNull()?.takeIf { it.isNotBlank() }
            host == "youtube.com" || host.endsWith(".youtube.com") -> {
                when (segments.firstOrNull()) {
                    "watch" -> uri.getQueryParameter("v")?.takeIf { it.isNotBlank() }
                    "shorts", "embed", "live", "v" -> segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            else -> null
        }
    }

    private suspend fun resolveSpotify(url: String) {
        // Fetch metadata via Spotify's public oEmbed service
        val oembedUrl = "https://open.spotify.com/oembed?url=${Uri.encode(url)}"
        val request = Request.Builder()
            .url(oembedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val response = runCatching { http.newCall(request).execute() }.getOrNull()
        // use{} guarantees the connection returns to the pool even when the
        // body is abandoned; without it a failed parse leaked the socket.
        val jsonString = response?.use { it.body?.string().orEmpty() } ?: ""

        var title = ""
        var artist = ""
        var artworkUrl: String? = null

        if (jsonString.isNotBlank()) {
            runCatching {
                val obj = json.parseToJsonElement(jsonString).jsonObject
                val rawTitle = obj["title"]?.jsonPrimitive?.content.orEmpty()
                artworkUrl = obj["thumbnail_url"]?.jsonPrimitive?.content

                if (rawTitle.contains(" - ")) {
                    title = rawTitle.substringBefore(" - ").trim()
                    artist = rawTitle.substringAfter(" - ").trim()
                } else if (rawTitle.contains(" by ")) {
                    title = rawTitle.substringBefore(" by ").trim()
                    artist = rawTitle.substringAfter(" by ").trim()
                } else {
                    title = rawTitle
                    artist = obj["author_name"]?.jsonPrimitive?.content.orEmpty()
                }
            }
        }

        if (title.isBlank()) {
            title = url.substringAfterLast('/').substringBefore('?')
        }

        // Match the song with exact high quality audio
        val match = runCatching { innerTube.findBestMatch(title, artist) }.getOrNull()
        val playable = if (match != null) {
            PlayableTrack(
                title = title.ifBlank { match.title },
                artist = artist.ifBlank { match.artist },
                album = match.album,
                artworkUrl = artworkUrl ?: match.artworkUrl,
                videoId = match.videoId,
                durationMs = match.durationSeconds?.takeIf { it > 0 }?.times(1_000L),
                explicit = match.explicit,
            )
        } else {
            PlayableTrack(
                title = title,
                artist = artist.ifBlank { "Spotify" },
                artworkUrl = artworkUrl,
            )
        }

        withContext(Dispatchers.Main) {
            musicPlayer.play(playable, sourceLabel = "Spotify Link", startRadio = true)
        }
    }

    private suspend fun resolveGenericQuery(query: String) {
        val tracks = innerTube.searchSongs(query, limit = 1)
        val first = tracks.firstOrNull() ?: return
        withContext(Dispatchers.Main) {
            musicPlayer.play(first.toPlayable(), sourceLabel = "Shared Song", startRadio = true)
        }
    }

    private fun YouTubeMusicTrack.toPlayable(): PlayableTrack = PlayableTrack(
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        videoId = videoId,
        durationMs = durationSeconds?.takeIf { it > 0 }?.times(1_000L),
        explicit = explicit,
    )
}
