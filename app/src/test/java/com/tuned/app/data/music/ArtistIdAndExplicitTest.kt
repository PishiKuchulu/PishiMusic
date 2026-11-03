package com.tuned.app.data.music

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistIdAndExplicitTest {
    private fun obj(text: String) = Json.parseToJsonElement(text).jsonObject

    private fun artistRun(name: String, id: String, pageType: String = "MUSIC_PAGE_TYPE_ARTIST") =
        """{"text":"$name","navigationEndpoint":{"browseEndpoint":{"browseId":"$id",
        "browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"$pageType"}}}}}"""

    @Test
    fun remembersArtistsCreditedInAnAlbumStrapline() {
        // VULTURES 2: search can't find "¥$", but the album header names its channel.
        val header = obj(
            """{"musicResponsiveHeaderRenderer":{"straplineTextOne":{"runs":[
            ${artistRun("¥$", "UCI-4sezWoZmtJXoW7E5k7Zw")},{"text":", "},
            ${artistRun("Kanye West", "UCRY5dYsbIN5TylSbd7gVnZg")}]}}}""",
        )
        val memory = ArtistIdMemory()
        memory.remember(header)
        assertEquals("UCI-4sezWoZmtJXoW7E5k7Zw", memory.idFor("¥$"))
        assertEquals("UCRY5dYsbIN5TylSbd7gVnZg", memory.idFor(" kanye west "))
        assertNull(memory.idFor(", "))
    }

    @Test
    fun ignoresChannelsThatAreNotArtistPages() {
        val memory = ArtistIdMemory()
        memory.remember(obj("""{"runs":[${artistRun("Some Uploader", "UCabc", "MUSIC_PAGE_TYPE_USER_CHANNEL")}]}"""))
        assertNull(memory.idFor("Some Uploader"))
    }

    @Test
    fun dropsTheOldestNamesPastCapacity() {
        val memory = ArtistIdMemory(capacity = 2)
        listOf("A" to "UC1", "B" to "UC2", "C" to "UC3").forEach { (name, id) ->
            memory.remember(obj("""{"runs":[${artistRun(name, id)}]}"""))
        }
        assertNull(memory.idFor("A"))
        assertEquals("UC3", memory.idFor("C"))
    }

    @Test
    fun readsTheExplicitBadgeFromRowsCardsAndHeaders() {
        val badge = """[{"musicInlineBadgeRenderer":{"icon":{"iconType":"MUSIC_EXPLICIT_BADGE"}}}]"""
        assertTrue(obj("""{"badges":$badge}""").hasExplicitBadge())
        assertTrue(obj("""{"subtitleBadges":$badge}""").hasExplicitBadge())
        assertTrue(obj("""{"subtitleBadge":$badge}""").hasExplicitBadge())
        assertFalse(obj("""{"badges":[{"musicInlineBadgeRenderer":{"icon":{"iconType":"OFFLINE_PIN"}}}]}""").hasExplicitBadge())
        assertFalse(obj("""{"title":"clean"}""").hasExplicitBadge())
    }
}
