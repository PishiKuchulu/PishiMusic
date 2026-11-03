package com.tuned.app.data.music

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Artist channel ids seen in YouTube Music responses, by credited name.
 *
 * Some names can't be found by search at all ("¥$" returns nothing), so an artist opened by
 * name from a song or album credit had no way to its page. Every song row and album header
 * already names its artists with their channel ids, so they are kept here as they arrive.
 */
internal class ArtistIdMemory(private val capacity: Int = 4_000) {
    private val ids = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > capacity
    }

    fun idFor(name: String): String? = synchronized(ids) { ids[key(name)] }

    fun remember(root: JsonElement) {
        val found = LinkedHashMap<String, String>()
        collectArtistRuns(root, found)
        if (found.isNotEmpty()) synchronized(ids) { ids.putAll(found) }
    }

    private fun key(name: String) = name.trim().lowercase()

    private fun collectArtistRuns(element: JsonElement, into: MutableMap<String, String>) {
        when (element) {
            is JsonObject -> {
                artistRun(element)?.let { (name, id) -> into[key(name)] = id }
                element.values.forEach { collectArtistRuns(it, into) }
            }
            is JsonArray -> element.forEach { collectArtistRuns(it, into) }
            else -> Unit
        }
    }

    private fun artistRun(run: JsonObject): Pair<String, String>? {
        val name = (run["text"] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val browse = (run["navigationEndpoint"] as? JsonObject)?.get("browseEndpoint") as? JsonObject ?: return null
        val id = (browse["browseId"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.startsWith("UC") } ?: return null
        val pageType = ((browse["browseEndpointContextSupportedConfigs"] as? JsonObject)
            ?.get("browseEndpointContextMusicConfig") as? JsonObject)
            ?.get("pageType") as? JsonPrimitive
        return if (pageType?.contentOrNull == "MUSIC_PAGE_TYPE_ARTIST") name to id else null
    }
}
