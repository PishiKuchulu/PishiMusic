package com.tuned.app.data.migration

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LegacyAccountReaderTest {

    private val cookie = stringPreferencesKey("innerTubeCookie")
    private val name = stringPreferencesKey("accountName")
    private val email = stringPreferencesKey("accountEmail")
    private val handle = stringPreferencesKey("accountChannelHandle")

    @Test
    fun readsASignedInAccount() {
        val account = LegacyAccountReader.from(
            mutablePreferencesOf(cookie to "SID=a; SAPISID=b; HSID=c", name to "Sam", handle to "@sam"),
        )
        assertThat(account).isEqualTo(LegacyAccount("SID=a; SAPISID=b; HSID=c", "Sam", "@sam"))
    }

    @Test
    fun fallsBackToTheEmailWhenThereIsNoName() {
        val account = LegacyAccountReader.from(mutablePreferencesOf(cookie to "SAPISID=b", email to "sam@example.com"))
        assertThat(account?.name).isEqualTo("sam@example.com")
        assertThat(account?.channelHandle).isNull()
    }

    @Test
    fun aCookieWithoutSapisidIsSignedOut() {
        // A visitor cookie is kept when signed out; it can't sign requests.
        assertThat(LegacyAccountReader.from(mutablePreferencesOf(cookie to "VISITOR_INFO1_LIVE=x"))).isNull()
        assertThat(LegacyAccountReader.from(mutablePreferencesOf())).isNull()
    }
}
