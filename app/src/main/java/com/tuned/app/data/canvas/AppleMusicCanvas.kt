package com.tuned.app.data.canvas

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import java.util.Locale

/**
 * Apple Music's motion artwork — animated sleeves exposed on the catalog API as `editorialVideo`.
 *
 * Scrapes an anonymous guest JWT from Apple Music Web Player bundles, queries
 * the AMP catalog search with `extend=editorialVideo`, ranks and validates results
 * against artist/title/album, and returns square/portrait HLS video streams.
 */
/** Apple Music's artist profile (see [AppleMusicCanvas.artistProfile]). */
data class AppleArtistProfile(
    /** Artwork URL template with {w}, {h} and {f}. */
    val photoTemplate: String?,
    /** RRGGBB, the colour Apple Music paints the artist page. */
    val bgColor: String?,
    val bio: String?,
    val origin: String?,
    /** ISO date ("1965-08-30") or a year. */
    val bornOrFormed: String?,
    val genres: List<String>,
    val url: String?,
    /** True for bands ("Formed"), false for solo artists ("Born"), null when Apple doesn't say. */
    val isGroup: Boolean? = null,
    /** Apple's catalog id, for the concerts on its web page. */
    val id: String? = null,
    /** The artist's wordmark as Apple Music shows it (trimmed PNG), when Apple has one. */
    val logoUrl: String? = null,
) {
    fun photoUrl(size: Int = 1200): String? =
        photoTemplate?.replace("{w}", "$size")?.replace("{h}", "$size")?.replace("{f}", "jpg")
}

/** One upcoming concert from Apple Music's artist page. */
data class AppleConcert(
    val city: String,
    /** Apple's own line, e.g. "Pier 48 · Wed, Dec 30 · 8 PM". */
    val subtitle: String,
    val date: java.time.Instant?,
    val zone: java.time.ZoneId?,
    val url: String?,
)

/** An album, playlist or video in one of Apple Music's shelves under an album. */
data class AppleShelfItem(
    val title: String,
    val artist: String?,
    val year: String?,
    /** Artwork URL template with {w}, {h} and {f}. */
    val artworkTemplate: String?,
    val isVideo: Boolean,
) {
    fun artworkUrl(width: Int = 600): String? = artworkTemplate
        ?.replace("{w}", "$width")
        ?.replace("{h}", "${if (isVideo) width * 9 / 16 else width}")
        ?.replace("{f}", "jpg")
}

/** One of Apple's shelves under an album ("Other versions", "You might also like", ...). */
data class AppleShelf(val kind: Kind, val items: List<AppleShelfItem>) {
    enum class Kind { MusicVideos, MoreByArtist, OtherVersions, YouMightAlsoLike }
}

/**
 * What Apple Music shows on an album page and YouTube Music lacks: the notes, the full release
 * date, the ℗ line, the record label and the shelves under the tracklist.
 */
data class AppleAlbumInfo(
    /** ISO date, e.g. "2007-04-23". */
    val releaseDate: String?,
    val copyright: String?,
    val recordLabel: String?,
    /** Apple's editorial notes, HTML. */
    val notes: String?,
    val genre: String?,
    val shelves: List<AppleShelf>,
)

object AppleMusicCanvas {

    private const val TAG = "AppleMusicCanvas"
    private const val AMP = "https://amp-api.music.apple.com/v1/catalog"
    private const val WEB_PLAYER = "https://music.apple.com/us/new"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val storefront: String by lazy {
        Locale.getDefault().country.takeIf { it.length == 2 }?.lowercase(Locale.ROOT) ?: "us"
    }

    fun search(client: OkHttpClient, title: String, artist: String, album: String?): CanvasArtwork? {
        val bearer = token(client) ?: return null

        val term = buildString {
            if (!title.contains(artist, ignoreCase = true)) append(artist).append(' ')
            append(title)
            if (!album.isNullOrBlank() && !title.contains(album, ignoreCase = true)) {
                append(' ').append(album)
            }
        }

        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "songs")
            .addQueryParameter("limit", "10")
            .addQueryParameter("extend", "editorialVideo")
            .addQueryParameter("include", "albums")
            .build()
            .toString()

        val body = get(client, url, bearer) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val hits = root["results"]?.jsonObject
            ?.get("songs")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return null

        val ranked = hits.mapNotNull { hit ->
            val song = hit as? JsonObject ?: return@mapNotNull null
            val score = score(song, title, artist, album) ?: return@mapNotNull null
            score to song
        }.sortedByDescending { it.first }

        for ((score, song) in ranked) {
            if (score < MIN_SCORE) {
                Log.d(TAG, "No hit scored above $MIN_SCORE for '$title' (best was $score)")
                break
            }
            val attributes = song["attributes"]?.jsonObject ?: continue
            val songName = attributes["name"]?.jsonPrimitive?.contentOrNull
            val songArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull
            val albumName = attributes["albumName"]?.jsonPrimitive?.contentOrNull

            attributes["editorialVideo"]?.jsonObject?.let { video ->
                motionUrls(video)?.let { assets ->
                    Log.d(TAG, "Inline motion artwork found for '$songName'")
                    return CanvasArtwork(
                        url = assets.primary,
                        fallbackUrl = assets.alternate,
                        tallUrl = assets.tall,
                        wideUrl = assets.wide,
                        title = songName,
                        artist = songArtist,
                        album = albumName,
                        source = CanvasSource.APPLE,
                    )
                }
            }

            val albumId = albumId(song) ?: continue
            fetchAlbum(client, albumId, bearer, songName, songArtist)?.let { return it }
        }
        return null
    }

    fun searchAlbum(client: OkHttpClient, album: String, artist: String): CanvasArtwork? {
        val bearer = token(client) ?: return null
        val term = if (album.contains(artist, ignoreCase = true)) album else "$artist $album"

        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "albums")
            .addQueryParameter("limit", "10")
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(client, url, bearer) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val hits = root["results"]?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return null

        val ranked = hits.mapNotNull { hit ->
            val record = hit as? JsonObject ?: return@mapNotNull null
            val score = score(record, album, artist, album, albumIsSelf = true) ?: return@mapNotNull null
            score to record
        }.sortedByDescending { it.first }

        for ((score, record) in ranked) {
            if (score < MIN_SCORE) break
            val attributes = record["attributes"]?.jsonObject ?: continue
            val name = attributes["name"]?.jsonPrimitive?.contentOrNull
            if (name != null && isCompilation(name)) continue
            val video = attributes["editorialVideo"]?.jsonObject ?: continue
            val assets = motionUrls(video) ?: continue

            Log.d(TAG, "Motion artwork found for album '$name'")
            return CanvasArtwork(
                url = assets.primary,
                fallbackUrl = assets.alternate,
                tallUrl = assets.tall,
                wideUrl = assets.wide,
                title = name,
                artist = attributes["artistName"]?.jsonPrimitive?.contentOrNull,
                album = name,
                source = CanvasSource.APPLE,
            )
        }
        return null
    }

    /** Apple's album views, in the order Apple Music shows them under the tracklist. */
    private val ALBUM_VIEWS = linkedMapOf(
        "related-videos" to AppleShelf.Kind.MusicVideos,
        "more-by-artist" to AppleShelf.Kind.MoreByArtist,
        "other-versions" to AppleShelf.Kind.OtherVersions,
        "you-might-also-like" to AppleShelf.Kind.YouMightAlsoLike,
    )

    /**
     * Apple Music's album page details for [album] by [artist]: one catalog search (the same
     * title by the same artist, preferring the edition with [trackCount] tracks),
     * then the album with its notes and shelves. Null when Apple has no such album.
     */
    fun albumInfo(client: OkHttpClient, album: String, artist: String, trackCount: Int?): AppleAlbumInfo? {
        val bearer = token(client) ?: return null
        val wantTitle = album.withoutEdition().normalizeForMatch()
        val wantArtist = artist.normalizeForMatch()
        if (wantTitle.isBlank()) return null
        val searchUrl = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", "$album $artist".trim())
            .addQueryParameter("types", "albums")
            .addQueryParameter("limit", "10")
            .build()
            .toString()
        val hits = get(client, searchUrl, bearer)?.let { body ->
            runCatching {
                json.parseToJsonElement(body).jsonObject["results"]?.jsonObject
                    ?.get("albums")?.jsonObject?.get("data")?.jsonArray
            }.getOrNull()
        } ?: return null
        val candidates = hits.mapNotNull { it as? JsonObject }.filter { hit ->
            val a = hit["attributes"]?.jsonObject ?: return@filter false
            val name = a["name"]?.jsonPrimitive?.contentOrNull.orEmpty().withoutEdition().normalizeForMatch()
            val by = a["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty().normalizeForMatch()
            name == wantTitle && (wantArtist.isBlank() || by == wantArtist || by.contains(wantArtist) || wantArtist.contains(by))
        }
        val best = candidates.firstOrNull {
            trackCount != null && it["attributes"]?.jsonObject?.get("trackCount")?.jsonPrimitive?.contentOrNull?.toIntOrNull() == trackCount
        } ?: candidates.firstOrNull() ?: return null
        val id = best["id"]?.jsonPrimitive?.contentOrNull ?: return null

        val albumUrl = "$AMP/$storefront/albums/$id".toHttpUrl().newBuilder()
            .addQueryParameter("views", ALBUM_VIEWS.keys.joinToString(","))
            .addQueryParameter("extend", "editorialNotes")
            .build()
            .toString()
        val record = get(client, albumUrl, bearer)?.let { body ->
            runCatching { json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject }.getOrNull()
        } ?: best
        val attributes = record["attributes"]?.jsonObject ?: return null
        fun text(key: String) = attributes[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
        val notes = attributes["editorialNotes"]?.jsonObject?.let { n ->
            (n["standard"] ?: n["short"])?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
        }
        val views = record["views"]?.jsonObject
        val shelves = ALBUM_VIEWS.mapNotNull { (key, kind) ->
            val items = views?.get(key)?.jsonObject?.get("data")?.jsonArray
                ?.mapNotNull { shelfItem(it as? JsonObject ?: return@mapNotNull null) }
                .orEmpty()
            items.takeIf { it.isNotEmpty() }?.let { AppleShelf(kind, it) }
        }
        return AppleAlbumInfo(
            releaseDate = text("releaseDate"),
            copyright = text("copyright"),
            recordLabel = text("recordLabel"),
            notes = notes,
            genre = attributes["genreNames"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                ?.takeIf { it != "Music" },
            shelves = shelves,
        )
    }

    private fun shelfItem(item: JsonObject): AppleShelfItem? {
        val a = item["attributes"]?.jsonObject ?: return null
        val type = item["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val name = a["name"]?.jsonPrimitive?.contentOrNull ?: return null
        return AppleShelfItem(
            title = name,
            artist = a["artistName"]?.jsonPrimitive?.contentOrNull,
            year = a["releaseDate"]?.jsonPrimitive?.contentOrNull?.take(4)?.takeIf { it.length == 4 },
            artworkTemplate = a["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull,
            isVideo = type == "music-videos",
        )
    }

    /** "Album (Deluxe Edition)" and "Album - Single" match "Album". */
    private fun String.withoutEdition(): String = replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
        .replace(Regex("""\s+-\s+(single|ep)$""", RegexOption.IGNORE_CASE), "")
        .trim()

    /**
     * Searches Apple Music catalog for an artist's official editorial motion artwork video.
     */
    fun searchArtist(client: OkHttpClient, artist: String): CanvasArtwork? {
        val bearer = token(client) ?: return null
        val term = artist.trim()
        if (term.isBlank()) return null

        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "artists")
            .addQueryParameter("limit", "5")
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(client, url, bearer) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val hits = root["results"]?.jsonObject
            ?.get("artists")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return null

        val wantNorm = artist.normalizeForMatch()

        for (hit in hits) {
            val artistObj = hit as? JsonObject ?: continue
            val attributes = artistObj["attributes"]?.jsonObject ?: continue
            val name = attributes["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val hitNorm = name.normalizeForMatch()
            if (hitNorm != wantNorm && !hitNorm.contains(wantNorm) && !wantNorm.contains(hitNorm)) {
                continue
            }

            // 1. Direct editorialVideo on artist object in search results
            attributes["editorialVideo"]?.jsonObject?.let { video ->
                motionUrls(video)?.let { assets ->
                    Log.d(TAG, "Motion artwork found for artist '$name'")
                    return CanvasArtwork(
                        url = assets.primary,
                        fallbackUrl = assets.alternate,
                        tallUrl = assets.tall,
                        wideUrl = assets.wide,
                        title = name,
                        artist = name,
                        source = CanvasSource.APPLE,
                    )
                }
            }

            // 2. Query artist detail endpoint if editorialVideo wasn't returned in search summary
            val artistId = artistObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
            fetchArtistDetail(client, artistId, bearer, name)?.let { return it }
        }
        return null
    }

    /**
     * Apple Music's own artist profile: the photo, the colours Apple paints the artist page in,
     * and the About details. Only an exact name match counts; anything else returns null.
     */
    fun artistProfile(client: OkHttpClient, artist: String): AppleArtistProfile? {
        val bearer = token(client) ?: return null
        val term = artist.trim()
        if (term.isBlank()) return null
        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "artists")
            .addQueryParameter("limit", "5")
            .addQueryParameter("extend", "artistBio,origin,bornOrFormed,isGroup,editorialArtwork")
            .build()
            .toString()
        val body = get(client, url, bearer) ?: return null
        val hits = runCatching {
            json.parseToJsonElement(body).jsonObject["results"]?.jsonObject
                ?.get("artists")?.jsonObject?.get("data")?.jsonArray
        }.getOrNull() ?: return null
        // Apple's top hit only, and only with this exact name: same-name knock-off profiles are
        // common, and Apple ranks the real (popular) artist first.
        val want = term.normalizeForMatch()
        val hit = (hits.firstOrNull() as? JsonObject)?.takeIf { obj ->
            obj["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull?.normalizeForMatch() == want
        } ?: return null
        val attributes = hit["attributes"]?.jsonObject ?: return null
        fun text(key: String) = attributes[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
        val artwork = attributes["artwork"]?.jsonObject
        fun artworkText(key: String) = artwork?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotEmpty)
        return AppleArtistProfile(
            photoTemplate = artworkText("url"),
            bgColor = artworkText("bgColor"),
            bio = text("artistBio"),
            origin = text("origin"),
            bornOrFormed = text("bornOrFormed"),
            genres = attributes["genreNames"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            url = text("url"),
            isGroup = attributes["isGroup"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull(),
            id = hit["id"]?.jsonPrimitive?.contentOrNull,
            logoUrl = attributes["editorialArtwork"]?.jsonObject?.get("musicContentColorLogoTrimmed")?.jsonObject
                ?.let(::logoUrl),
        )
    }

    /** A wordmark template at up to 1000px wide, as PNG so it keeps its transparency. */
    private fun logoUrl(logo: JsonObject): String? {
        val template = logo["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val w = logo["width"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
        val h = logo["height"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
        val outW = w.coerceAtMost(1000)
        val outH = (outW.toLong() * h / w).toInt().coerceAtLeast(1)
        return template.replace("{w}", "$outW").replace("{h}", "$outH")
            .replace(Regex("""\.(\{f\}|jpe?g|webp)$"""), ".png")
    }

    /**
     * Apple's photo template for [artist], "" when Apple's top hit isn't this exact name (use
     * YouTube's picture), or null when the request failed (ask again later).
     */
    fun artistPhotoTemplate(client: OkHttpClient, artist: String): String? {
        val bearer = token(client) ?: return null
        val term = artist.trim()
        if (term.isBlank()) return ""
        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "artists")
            .addQueryParameter("limit", "5")
            .build()
            .toString()
        val body = get(client, url, bearer) ?: return null
        val top = runCatching {
            json.parseToJsonElement(body).jsonObject["results"]?.jsonObject
                ?.get("artists")?.jsonObject?.get("data")?.jsonArray?.firstOrNull()?.jsonObject
        }.getOrNull() ?: return ""
        val attributes = top["attributes"]?.jsonObject ?: return ""
        if (attributes["name"]?.jsonPrimitive?.contentOrNull?.normalizeForMatch() != term.normalizeForMatch()) return ""
        return attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    /**
     * Upcoming concerts from the "All Upcoming Concerts" shelf of Apple's public artist page
     * (its server-rendered data). Empty when there are none or the page changed shape.
     */
    fun upcomingConcerts(client: OkHttpClient, artistId: String): List<AppleConcert> {
        val html = canvasGet(client, "https://music.apple.com/$storefront/artist/$artistId", mapOf("User-Agent" to CANVAS_UA))
            ?: return emptyList()
        val data = Regex("""<script[^>]*id="serialized-server-data"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return emptyList()
        val shelf = runCatching { findConcertShelf(json.parseToJsonElement(data)) }.getOrNull() ?: return emptyList()
        return shelf.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val city = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val artwork = obj["artwork"] as? JsonObject
            AppleConcert(
                city = city,
                subtitle = obj["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty().replace('\u00A0', ' '),
                date = artwork?.get("date")?.jsonPrimitive?.contentOrNull
                    ?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() },
                zone = artwork?.get("timeZone")?.jsonPrimitive?.contentOrNull
                    ?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() },
                url = (obj["contentDescriptor"] as? JsonObject)?.get("url")?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    /** Apple's page for all of the artist's upcoming concerts (tickets and the rest). */
    fun concertsPageUrl(artistId: String): String = "https://music.apple.com/$storefront/concerts/artist/$artistId"

    private fun findConcertShelf(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonArray? = when (element) {
        is JsonObject -> {
            val id = (element["id"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull.orEmpty()
            val items = element["items"] as? kotlinx.serialization.json.JsonArray
            if (id.contains("AllUpcomingConcerts") && items != null) items
            else element.values.firstNotNullOfOrNull(::findConcertShelf)
        }
        is kotlinx.serialization.json.JsonArray -> element.firstNotNullOfOrNull(::findConcertShelf)
        else -> null
    }

    private fun fetchArtistDetail(
        client: OkHttpClient,
        artistId: String,
        bearer: String,
        artistName: String,
    ): CanvasArtwork? {
        val url = "$AMP/$storefront/artists/$artistId".toHttpUrl().newBuilder()
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(client, url, bearer) ?: return null
        val artistObj = runCatching {
            json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
        }.getOrNull() ?: return null

        val attributes = artistObj["attributes"]?.jsonObject
        val video = attributes?.get("editorialVideo")?.jsonObject
        val directAssets = video?.let(::motionUrls)
        if (directAssets != null) {
            Log.d(TAG, "Motion artwork found on artist detail '$artistName' ($artistId)")
            return CanvasArtwork(
                url = directAssets.primary,
                fallbackUrl = directAssets.alternate,
                tallUrl = directAssets.tall,
                wideUrl = directAssets.wide,
                title = artistName,
                artist = artistName,
                source = CanvasSource.APPLE,
            )
        }

        // No artist video: Apple Music shows the artist photo then, not an album's motion cover.
        return null
    }

    private const val MIN_SCORE = 12

    private fun score(
        song: JsonObject,
        title: String,
        artist: String,
        album: String?,
        albumIsSelf: Boolean = false,
    ): Int? {
        val attributes = song["attributes"]?.jsonObject ?: return null
        val hitName = attributes["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val hitArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val hitAlbum = if (albumIsSelf) {
            hitName
        } else {
            attributes["albumName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }

        if (isCompilation(hitName) || isCompilation(hitAlbum)) return null

        val wanted = splitArtists(artist)
        val credited = splitArtists(hitArtist)
        if (wanted.isEmpty() || credited.isEmpty()) return null
        if (!wanted.all { want -> credited.any { it == want || it.contains(want) || want.contains(it) } }) return null

        var score = 10

        val wantTitle = title.normalizeForMatch()
        val hitTitle = hitName.normalizeForMatch()
        score += when {
            hitTitle == wantTitle -> 15
            hitTitle.contains(wantTitle) || wantTitle.contains(hitTitle) -> 7
            else -> -10
        }

        if (!album.isNullOrBlank() && hitAlbum.isNotBlank()) {
            val wantAlbum = album.normalizeForMatch()
            val gotAlbum = hitAlbum.normalizeForMatch()
            score += when {
                gotAlbum == wantAlbum -> 20
                gotAlbum.contains(wantAlbum) || wantAlbum.contains(gotAlbum) -> 10
                else -> 0
            }
        }

        for (word in EDITION_WORDS) {
            val inWanted = title.contains(word, ignoreCase = true)
            val inHit = hitName.contains(word, ignoreCase = true)
            if (inWanted && inHit) score += 5 else if (inHit) score -= 3
        }

        return score
    }

    private val EDITION_WORDS =
        listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")

    private fun isCompilation(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return COMPILATION_MARKERS.any { lower.contains(it) }
    }

    private val COMPILATION_MARKERS = listOf(
        "playlist", "set list", "essentials", "dj mix", "mixed",
        "apple music", "today's hits", "session",
    )

    private fun albumId(song: JsonObject): String? {
        val fromRelationship = song["relationships"]?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        if (fromRelationship != null) return fromRelationship.takeUnless { it.startsWith("pl.") }

        val url = song["attributes"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: return null
        return url.substringAfter("/album/", "")
            .substringBefore("?")
            .substringAfterLast("/")
            .takeIf { it.isNotBlank() && it.all(Char::isDigit) }
    }

    private fun fetchAlbum(
        client: OkHttpClient,
        albumId: String,
        bearer: String,
        songTitle: String?,
        songArtist: String?,
    ): CanvasArtwork? {
        val url = "$AMP/$storefront/albums/$albumId".toHttpUrl().newBuilder()
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(client, url, bearer) ?: return null
        val album = runCatching {
            json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
        }.getOrNull() ?: return null

        val attributes = album["attributes"]?.jsonObject ?: return null
        val albumName = attributes["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (isCompilation(albumName)) return null

        val video = attributes["editorialVideo"]?.jsonObject ?: return null
        val assets = motionUrls(video) ?: return null

        Log.d(TAG, "Motion artwork found on album '$albumName' ($albumId)")
        return CanvasArtwork(
            url = assets.primary,
            fallbackUrl = assets.alternate,
            tallUrl = assets.tall,
            wideUrl = assets.wide,
            title = songTitle,
            artist = songArtist ?: attributes["artistName"]?.jsonPrimitive?.contentOrNull,
            album = albumName,
            source = CanvasSource.APPLE,
        )
    }

    private data class MotionUrls(
        val primary: String,
        val alternate: String?,
        val tall: String?,
        val wide: String?,
    )

    private fun motionUrls(video: JsonObject): MotionUrls? {
        fun link(key: String): String? = video[key]?.jsonObject?.let { asset ->
            asset["video"]?.jsonPrimitive?.contentOrNull
                ?: asset["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["url"]?.jsonPrimitive?.contentOrNull
        }?.takeIf { it.isNotBlank() }

        val square = link("motionDetailSquare")
            ?: link("motionSquareVideo1x1")
            ?: link("motionArtistSquare1x1")
            ?: link("motionArtistSquare")
        val raw = link("motionDetailRaw")
        val tall = link("motionDetailTall")
            ?: link("motionTallVideo3x4")
            ?: link("motionArtistFullscreen16x9")
            ?: link("motionArtistTall")
            ?: link("motionArtistFullscreen")
        val wide = link("motionWideVideo")
            ?: link("motionWideVideo16x9")
            ?: link("motionDetailWide")
            ?: link("motionArtistWide16x9")
            ?: link("motionArtistWide")
        val fallback = video.keys.firstNotNullOfOrNull { link(it) }
        val primary = square ?: raw ?: tall ?: wide ?: fallback ?: return null
        val alternate = listOfNotNull(square, raw, tall, wide).firstOrNull { it != primary }
        return MotionUrls(primary, alternate, tall, wide)
    }

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAtMs = 0L
    @Volatile private var retryTokenAfterMs = 0L
    private val rejected = mutableSetOf<String>()

    @Synchronized
    private fun token(client: OkHttpClient): String? {
        val now = System.currentTimeMillis()
        cachedToken?.let { if (now < tokenExpiresAtMs - 60_000) return it }
        if (now < retryTokenAfterMs) return null

        val html = canvasGet(client, WEB_PLAYER, mapOf("User-Agent" to CANVAS_UA))
            ?: canvasGet(client, "https://music.apple.com/us/browse", mapOf("User-Agent" to CANVAS_UA))
        val scripts = html?.let {
            Regex("""/assets/(?:index|app)(?:-legacy)?[~-][A-Za-z0-9_-]+\.js|/assets/index[~-][A-Za-z0-9_-]+\.js""")
                .findAll(it).map(MatchResult::value).distinct().toList()
        }.orEmpty()

        for (path in scripts) {
            val script = canvasGet(client, "https://music.apple.com$path", mapOf("User-Agent" to CANVAS_UA))
                ?: continue
            val candidates = Regex("""ey[A-Za-z0-9_-]+\.ey[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
                .findAll(script)
                .map(MatchResult::value)
                .distinct()
                .filter { it !in rejected }
                .mapNotNull { jwt -> expiry(jwt)?.let { jwt to it } }
                .filter { it.second > now }
                .toList()
            if (candidates.isEmpty()) continue

            val (jwt, expiresAt) = candidates.firstOrNull { isWebPlayerToken(it.first) }
                ?: candidates.first()
            cachedToken = jwt
            tokenExpiresAtMs = expiresAt
            Log.d(TAG, "Apple Music web player token good until ${java.util.Date(expiresAt)}")
            return jwt
        }

        Log.w(TAG, "No usable web player token in ${scripts.size} bundle(s); backing off")
        retryTokenAfterMs = now + TOKEN_RETRY_MS
        return null
    }

    private const val TOKEN_RETRY_MS = 30L * 60 * 1000

    private fun get(client: OkHttpClient, url: String, bearer: String): String? {
        val request = Request.Builder().url(url).apply {
            authHeaders(bearer).forEach { (name, value) -> header(name, value) }
        }.build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> response.body?.string()
                    response.code == 401 -> {
                        Log.w(TAG, "Apple Music token rejected (401); will re-scrape")
                        synchronized(this) {
                            rejected += bearer
                            if (cachedToken == bearer) {
                                cachedToken = null
                                tokenExpiresAtMs = 0L
                            }
                        }
                        null
                    }
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun isWebPlayerToken(jwt: String): Boolean = runCatching {
        val parts = jwt.split(".")
        val header = String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        header.contains("WebPlayKid") || payload.contains("AMPWebPlay")
    }.getOrDefault(false)

    private fun expiry(jwt: String): Long? = runCatching {
        val payload = String(
            Base64.getUrlDecoder().decode(jwt.split(".")[1]),
            Charsets.UTF_8,
        )
        val seconds = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payload)?.groupValues?.get(1)
        seconds?.toLong()?.times(1000)
    }.getOrDefault(null)

    private fun authHeaders(bearer: String) = mapOf(
        "Authorization" to "Bearer $bearer",
        "Origin" to "https://music.apple.com",
        "Referer" to "https://music.apple.com/",
        "User-Agent" to CANVAS_UA,
    )
}
