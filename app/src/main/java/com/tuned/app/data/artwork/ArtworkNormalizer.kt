package com.tuned.app.data.artwork

import com.tuned.app.data.model.ImageDto

/**
 * Faithful port of app.js's image-selection helpers (_isRealImg /
 * _isRealHomeImg and the repeated
 * `find(extralarge) || find(large) || find(medium) || find(any)` chain used
 * everywhere a Last.fm image array is read). Kept as free functions, not a
 * class, because the original has no state here — just filtering.
 */
object ArtworkNormalizer {

    /** The exact hash Last.fm embeds in its own gray "no artwork" placeholder
     *  image. Last.fm returns this as a real, non-blank URL, so a blank
     *  check alone isn't enough to tell real art from a placeholder — this
     *  is the check app.js's _isRealImg() does before trusting any image URL. */
    private const val LASTFM_NO_ART_HASH = "2a96cbd8b46e442fc41c2b86b821562f"

    fun isRealImage(url: String?): Boolean =
        !url.isNullOrBlank() && !url.contains(LASTFM_NO_ART_HASH)

    /** A 16:9 YouTube video frame (i.ytimg.com), not a square album cover. */
    fun isVideoThumbnail(url: String?): Boolean =
        url != null && (url.contains("i.ytimg.com/") || url.contains("ytimg.com/vi"))

    /** YouTube Music's own square art for a release, as opposed to a video frame. */
    fun isYouTubeMusicCover(url: String?): Boolean =
        url != null && (url.contains("googleusercontent.com/") || url.contains("ggpht.com/"))

    /**
     * The picture to show for a track, or null while there is nothing right to
     * show yet. Covers are square: a local file, then YouTube Music's own art for the
     * release the track was played from, then the cover found online. The lookup goes by
     * title and artist, so it can't tell an album track from its single. A video thumbnail is
     * only used once the lookup confirms there is no album cover anywhere
     * (the track really is a YouTube video).
     *
     * [resolved] follows the repository's map: null = not looked up yet,
     * "" = no art anywhere.
     */
    fun coverFor(
        embeddedUrl: String?,
        resolved: String?,
        resolvedFailed: Boolean = false,
        embeddedFailed: Boolean = false,
    ): String? {
        if (embeddedUrl?.startsWith("file://") == true && !embeddedFailed) return embeddedUrl
        if (isYouTubeMusicCover(embeddedUrl) && !embeddedFailed) return upscaleYoutubeArtwork(embeddedUrl)
        if (!resolved.isNullOrBlank() && !resolvedFailed) return resolved
        if (!isRealImage(embeddedUrl) || embeddedFailed) return null
        val noAlbumCover = resolved == "" || resolvedFailed
        return if (!isVideoThumbnail(embeddedUrl) || noAlbumCover) upscaleYoutubeArtwork(embeddedUrl) else null
    }

    /** extralarge > large > medium > (any remaining real image) — the exact
     *  priority order used at every image-array read site in app.js/home.js. */
    fun bestImageUrl(images: List<ImageDto>): String? {
        val bySize = { size: String -> images.firstOrNull { it.size == size && isRealImage(it.url) }?.url }
        return bySize("extralarge")
            ?: bySize("large")
            ?: bySize("medium")
            ?: images.firstOrNull { isRealImage(it.url) }?.url
    }

    /** `t:${name}|${artist}`.toLowerCase() — the exact cache key format
     *  _resolveTrackArt() uses. No punctuation/feat./remaster stripping:
     *  the original app doesn't do any of that, so neither does this. */
    fun cacheKey(name: String, artist: String): String = "t:${name}|${artist}".lowercase()

    private val FEAT_REGEX = Regex("(?i)\\s*[(|\\[](feat|ft|with|featuring)\\.?\\s+.*?[)|\\]]")
    private val REMASTER_REGEX = Regex("(?i)\\s*[(|\\[].*?(remaster|live|version|edit|mono|stereo|deluxe|bonus).*?[)|\\]]")

    val HIGH_RES_PROVIDERS: Set<String> = setOf("spotify", "apple", "itunes", "tidal", "deezer")

    private val YT_SIZE_REGEX = Regex("""=w\d+-h\d+[^?#]*|=s\d+[^?#]*""")

    /**
     * Upscales YouTube / Googleusercontent thumbnails from compressed low-res
     * dimensions (e.g. w120-h120 or s544) to high-resolution master dimensions (s1200 or maxresdefault).
     */
    fun upscaleYoutubeArtwork(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!isRealImage(url)) return url
        var result = url
        if (result.contains("googleusercontent.com") || result.contains("ggpht.com")) {
            result = if (YT_SIZE_REGEX.containsMatchIn(result)) {
                result.replace(YT_SIZE_REGEX, "=s1200")
            } else {
                "$result=s1200"
            }
        } else if (result.contains("i.ytimg.com/vi/")) {
            result = result.replace("/hqdefault.jpg", "/maxresdefault.jpg")
                .replace("/sddefault.jpg", "/maxresdefault.jpg")
        }
        return result
    }

    fun cleanTitle(title: String): String = title
        .replace(FEAT_REGEX, "")
        .replace(REMASTER_REGEX, "")
        .trim()

    fun cleanArtist(artist: String): String = artist
        .replace(Regex("(?i)\\s*(feat|ft|with|&|,|/|x)\\s+.*"), "")
        .trim()
}
