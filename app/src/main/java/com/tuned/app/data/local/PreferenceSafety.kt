package com.tuned.app.data.local

import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen

/**
 * Reads by name and verifies the runtime type before returning a value.
 *
 * Preferences.Key equality intentionally uses only the key name. If a legacy
 * or edited backup restores that name with a different value type, the normal
 * typed operator can throw ClassCastException at the call site. This helper
 * turns such values into an ordinary missing preference so defaults apply.
 */
internal inline fun <reified T> Preferences.readSafely(key: Preferences.Key<T>): T? =
    asMap().entries.firstOrNull { it.key.name == key.name }?.value as? T

/**
 * A transient filesystem/read failure must not escape a long-lived collector.
 * DataStore's corruption handler repairs malformed protobuf files; this is the
 * final boundary for other read failures and emits defaults for this session.
 *
 * The previous shape was a plain catch { emit(emptyPreferences()) }: the flow
 * then COMPLETED, so a single transient read error froze every long-lived
 * collector (settings/session/theme streams) at defaults for the rest of the
 * process — later real preference changes never arrived. Now transient
 * failures retry with backoff first and only persistent failure degrades to
 * session defaults.
 */
internal fun Flow<Preferences>.recoverPreferences(owner: String): Flow<Preferences> =
    retryWhen { cause, attempt ->
        if (cause is kotlinx.coroutines.CancellationException) throw cause
        if (attempt >= MAX_PREFERENCE_READ_RETRIES) {
            Log.e(owner, "Preferences unavailable after retries; using safe defaults", cause)
            false
        } else {
            Log.w(owner, "Preferences read failed (attempt $attempt); retrying", cause)
            kotlinx.coroutines.delay(200L * (attempt + 1))
            true
        }
    }.catch { error ->
        Log.e(owner, "Preferences unavailable; using safe defaults", error)
        emit(emptyPreferences())
    }

private const val MAX_PREFERENCE_READ_RETRIES = 3L
