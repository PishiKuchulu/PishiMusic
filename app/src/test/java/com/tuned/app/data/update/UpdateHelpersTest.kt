package com.tuned.app.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateHelpersTest {
    @Test
    fun notesLoseTheirMarkdown() {
        val notes = plainReleaseNotes("## What's new\n\n- **Faster** search\n* A [link](https://example.com)\n\n\n\nDone `now`")
        assertThat(notes).isEqualTo("What's new\n\n• Faster search\n• A link\n\nDone now")
    }

    @Test
    fun onlyAFullSha256DigestIsTrusted() {
        val hash = "a".repeat(64)
        assertThat(sha256FromDigest("sha256:$hash")).isEqualTo(hash)
        assertThat(sha256FromDigest("sha256:abc")).isNull()
        assertThat(sha256FromDigest("md5:$hash")).isNull()
        assertThat(sha256FromDigest(null)).isNull()
    }
}
