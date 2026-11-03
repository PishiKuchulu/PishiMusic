package com.tuned.app.data.migration

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

/** The previous app's YouTube Music sign-in: the session cookie and who it belongs to. */
data class LegacyAccount(val cookie: String, val name: String, val channelHandle: String?)

/**
 * Reads the YouTube Music sign-in from the previous app's settings (a Preferences DataStore
 * named "settings"; Tuned's own stores use other names). Only reads.
 */
object LegacyAccountReader {

    private const val STORE = "settings"
    private val COOKIE = stringPreferencesKey("innerTubeCookie")
    private val NAME = stringPreferencesKey("accountName")
    private val EMAIL = stringPreferencesKey("accountEmail")
    private val HANDLE = stringPreferencesKey("accountChannelHandle")

    suspend fun read(context: Context): LegacyAccount? {
        val file = context.preferencesDataStoreFile(STORE)
        if (!file.exists()) return null
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            from(PreferenceDataStoreFactory.create(scope = scope) { file }.data.first())
        } finally {
            scope.cancel()
        }
    }

    /** The account in [prefs], or null when it wasn't signed in. */
    fun from(prefs: Preferences): LegacyAccount? {
        val cookie = prefs[COOKIE]?.trim().orEmpty()
        // Without SAPISID the cookie can't sign YouTube Music requests: treat it as signed out.
        if (cookie.isEmpty() || "SAPISID" !in cookie) return null
        val name = prefs[NAME]?.takeIf { it.isNotBlank() } ?: prefs[EMAIL].orEmpty()
        return LegacyAccount(cookie, name, prefs[HANDLE]?.takeIf { it.isNotBlank() })
    }
}
