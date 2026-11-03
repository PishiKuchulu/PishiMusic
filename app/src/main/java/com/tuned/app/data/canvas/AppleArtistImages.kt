package com.tuned.app.data.canvas

import android.content.Context
import android.content.SharedPreferences
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.size.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

/**
 * An artist picture to load: Apple Music's photo of [name] when Apple's top artist hit has that
 * exact name, otherwise [fallbackUrl] (YouTube Music's). Pass it as Coil's `data`; the
 * [AppleArtistImages.interceptor] picks the source before loading, so only one image is drawn.
 */
data class ArtistImage(val name: String, val fallbackUrl: String?)

/**
 * Apple Music's artist photos across the app (search, similar artists, Home, the artist page).
 * Answers are saved on the device, so each artist is looked up once.
 */
object AppleArtistImages {
    /** Past this, the YouTube picture is used for this load (the answer is still saved for next time). */
    private const val WAIT_MS = 1500L
    /** Saved answer for "Apple has no exact match: use YouTube's". */
    private const val NONE = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val known = ConcurrentHashMap<String, String>()
    private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()
    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var client: (() -> OkHttpClient)? = null

    fun init(context: Context, client: () -> OkHttpClient) {
        this.client = client
        val p = context.getSharedPreferences("apple_artist_photos", Context.MODE_PRIVATE)
        prefs = p
        // A few KB even for thousands of artists; loaded once so lookups are synchronous after.
        p.all.forEach { (k, v) -> (v as? String)?.let { known[k] = it } }
    }

    private fun keyOf(name: String) = name.removeSuffix(" - Topic").trim().lowercase()

    private fun lookupAsync(name: String): Deferred<String?> {
        val key = keyOf(name)
        return inFlight.computeIfAbsent(key) {
            scope.async {
                val http = client?.invoke() ?: return@async null
                val result = runCatching {
                    AppleMusicCanvas.artistPhotoTemplate(http, name.removeSuffix(" - Topic").trim())
                }.getOrNull()
                if (result != null) {
                    known[key] = result
                    prefs?.edit()?.putString(key, result)?.apply()
                }
                result
            }.also { d -> d.invokeOnCompletion { inFlight.remove(key, d) } }
        }
    }

    /** Starts the lookup ahead of display, e.g. while the artist page is still loading. */
    fun prefetch(name: String) {
        if (name.isNotBlank() && !known.containsKey(keyOf(name))) lookupAsync(name)
    }

    /** Apple photo URL at about [sizePx], bucketed so sizes share the image cache. */
    private fun appleUrl(template: String, sizePx: Int): String {
        val size = listOf(300, 600, 1200).firstOrNull { it >= sizePx } ?: 1200
        return template.replace("{w}", "$size").replace("{h}", "$size").replace("{f}", "jpg")
    }

    /** Apple's photo of [name], else [fallbackUrl]; waits up to [WAIT_MS] for a first lookup. */
    suspend fun urlFor(name: String, fallbackUrl: String?, sizePx: Int = 600): String? {
        val template = known[keyOf(name)] ?: withTimeoutOrNull(WAIT_MS) { lookupAsync(name).await() }
        return template?.takeIf { it != NONE }?.let { appleUrl(it, sizePx) } ?: fallbackUrl
    }

    /** Swaps an [ArtistImage] for the URL to load, before anything is fetched. */
    val interceptor = object : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val data = chain.request.data as? ArtistImage ?: return chain.proceed(chain.request)
            val sizePx = (chain.size.width as? Dimension.Pixels)?.px ?: 1200
            val url = urlFor(data.name, data.fallbackUrl, sizePx)
            return chain.proceed(chain.request.newBuilder().data(url).build())
        }
    }
}
