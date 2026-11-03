package com.tuned.app.playback

/** What a YouTube or YouTube Music link points at. */
sealed interface YouTubeLink {
    data class Album(val browseId: String) : YouTubeLink
    data class Playlist(val id: String) : YouTubeLink
    data class Artist(val browseId: String) : YouTubeLink
    /** One song or video, possibly inside a list it should play from. */
    data class Track(val videoId: String, val listId: String? = null) : YouTubeLink
}

private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")

/**
 * Reads a link into what it points at, or null when it isn't one of these. A link to a
 * list opens that list; only a link naming a song plays. YouTube Music shares albums as
 * `OLAK5uy_` playlists, so those count as albums, not playlists.
 */
fun parseYouTubeLink(url: String): YouTubeLink? {
    val clean = url.trim()
    val afterScheme = clean.substringAfter("://", "")
    if (afterScheme.isEmpty()) return null
    val hostAndPath = afterScheme.substringBefore('#')
    val host = hostAndPath.substringBefore('/').substringBefore('?').lowercase()
    if (host !in YOUTUBE_HOSTS) return null
    val path = hostAndPath.substringAfter('/', "").substringBefore('?').trim('/')
    val query = hostAndPath.substringAfter('?', "").split('&')
        .mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
        .toMap()
    val parts = path.split('/').filter(String::isNotBlank)
    val listId = query["list"]?.takeIf(String::isNotBlank)
    val videoId = when {
        host == "youtu.be" -> parts.firstOrNull()
        parts.firstOrNull() == "watch" -> query["v"] ?: parts.getOrNull(1)
        parts.firstOrNull() == "shorts" -> parts.getOrNull(1)
        else -> null
    }?.takeIf(String::isNotBlank)
    return when {
        videoId != null -> YouTubeLink.Track(videoId, listId)
        parts.firstOrNull() == "browse" -> parts.getOrNull(1)?.let(::browseTarget)
        parts.firstOrNull() == "channel" -> parts.getOrNull(1)?.let(YouTubeLink::Artist)
        listId != null -> listTarget(listId)
        else -> null
    }
}

private fun browseTarget(browseId: String): YouTubeLink? = when {
    browseId.startsWith("MPRE") -> YouTubeLink.Album(browseId)
    browseId.startsWith("VL") -> listTarget(browseId.removePrefix("VL"))
    browseId.startsWith("UC") -> YouTubeLink.Artist(browseId)
    else -> null
}

private fun listTarget(id: String): YouTubeLink =
    if (id.startsWith("OLAK")) YouTubeLink.Album(id) else YouTubeLink.Playlist(id)
