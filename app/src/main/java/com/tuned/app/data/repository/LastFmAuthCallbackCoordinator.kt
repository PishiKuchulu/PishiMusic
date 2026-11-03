package com.tuned.app.data.repository

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

const val LAST_FM_AUTH_CALLBACK_URI = "tuned://auth-callback"

/** Carries the browser redirect safely into the login ViewModel. */
@Singleton
class LastFmAuthCallbackCoordinator @Inject constructor() {
    private val _pendingToken = MutableStateFlow<String?>(null)
    val pendingToken: StateFlow<String?> = _pendingToken.asStateFlow()

    private var lastAcceptedToken: String? = null

    // Login-CSRF guard: a callback token is only honored while a web-auth
    // flow the USER started is pending, and only within a freshness window.
    // Previously any app or web page could fire tuned://auth-callback with
    // an attacker's token at any time and bind this install to the
    // attacker's Last.fm account.
    @Volatile
    private var armedAtMillis: Long = 0L

    fun arm() {
        armedAtMillis = System.currentTimeMillis()
    }

    @Synchronized
    fun capture(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        if (uri.scheme != "tuned" || uri.host != "auth-callback") return false

        if (armedAtMillis == 0L ||
            System.currentTimeMillis() - armedAtMillis > AUTH_FLOW_WINDOW_MS
        ) {
            return false
        }

        val token = uri.getQueryParameter("token")?.trim().orEmpty()
        if (token.isEmpty()) return false

        if (token != lastAcceptedToken) {
            lastAcceptedToken = token
            _pendingToken.value = token
        }
        return true
    }

    @Synchronized
    fun consume(token: String) {
        if (_pendingToken.value == token) {
            _pendingToken.value = null
            lastAcceptedToken = null
            armedAtMillis = 0L
        }
    }

    private companion object {
        const val AUTH_FLOW_WINDOW_MS = 10 * 60 * 1000L
    }
}
