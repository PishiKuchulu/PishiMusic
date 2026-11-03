package com.tuned.app.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tuned.app.data.local.db.SavedPlaylistDao
import com.tuned.app.data.local.db.SavedPlaylistEntity
import com.tuned.app.data.local.db.RecommendationExclusionDao
import com.tuned.app.data.local.db.RecommendationExclusionEntity
import com.tuned.app.data.local.readSafely
import com.tuned.app.data.playlist.PlaylistPublicMirror
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val SCHEMA_VERSION = 8
private const val BACKUP_TYPE = "tuned-backup"

/**
 * Preference keys whose on-disk type is Long. Backup schema <= 7 incorrectly
 * narrowed every Long to Int and restored it with an Int key. Preferences keys
 * are name-based, so reading one of those values through a Long key then throws
 * ClassCastException. Keep the names here both to repair old backups and to
 * normalise already-damaged preferences when they are backed up again.
 */
private val LONG_PREFERENCE_NAMES = setOf(
    "ytm_connected_at",
    "ytm_last_sync_at",
)

/**
 * Credentials and tokens that must never be written into an exported backup
 * file. Backups are plaintext JSON the user shares/copies around, so a leaked
 * cookie, API secret, session token, or addon URL (which embeds an access
 * token) would be a full account compromise. These keys are excluded from
 * buildBackup(); restoring therefore clears them and the user re-authenticates
 * (the web-auth restore flow preserves the signed-in session separately via
 * AUTH_PREFERENCE_NAMES).
 */
private val SENSITIVE_PREFERENCE_NAMES = setOf(
    "ytm_cookies",
    "lw_apikey",
    "lw_apisecret",
    "lw_sessionkey",
    "lw_addon_url",
    // Spotify session material stored in the same DataStore: the sp_dc login
    // cookie and both bearer tokens are full session credentials.
    "spotify_sp_dc",
    "spotify_personal_token",
    "spotify_client_token",
    // The multi-addon JSON embeds each addon's URL, and those URLs carry an
    // access token in their /a/<token>/ path — same exposure as lw_addon_url.
    "lw_http_addons_json",
    // YouTube delegated-account identity values are session-adjacent.
    "ytm_page_id",
    "ytm_on_behalf_of_user",
)


@Serializable
data class BackupPrefsSnapshot(
    val strings: Map<String, String> = emptyMap(),
    val booleans: Map<String, Boolean> = emptyMap(),
    val integers: Map<String, Int> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
    val stringSets: Map<String, Set<String>> = emptyMap(),
    // Float preferences (lyrics font scale, loudness preamp) were silently
    // dropped by older backups; the default keeps old files decodable.
    val floats: Map<String, Float> = emptyMap(),
)

@Serializable
data class BackupPlaylistSnapshot(
    val id: Long,
    val title: String,
    val subtitle: String,
    val mode: String,
    val tracksJson: String,
    val createdAtMillis: Long,
    val discoverSignature: String? = null,
    val customCoverUri: String? = null,
    val isPinned: Boolean = false,
)

/** Explicit recommendation exclusions were added to backup schema v7. */
@Serializable
data class BackupRecommendationExclusionSnapshot(
    val trackKey: String,
    val excludedAtMillis: Long,
    val trackName: String = "",
    val artistName: String = "",
)

@Serializable
data class BackupFile(
    val type: String = BACKUP_TYPE,
    val schemaVersion: Int = SCHEMA_VERSION,
    val createdAt: Long,
    val appVersion: String,
    val prefs: BackupPrefsSnapshot,
    val playlists: List<BackupPlaylistSnapshot>,
    val recommendationExclusions: List<BackupRecommendationExclusionSnapshot> = emptyList(),
)

sealed interface RestoreResult {
    data class Success(val playlistCount: Int, val exclusionCount: Int) : RestoreResult
    data object UnsupportedSchema : RestoreResult
    data object InvalidFile : RestoreResult
    data class Failed(val message: String) : RestoreResult
}

sealed interface BackupCheck {
    data class Valid(val playlistCount: Int) : BackupCheck
    data object UnsupportedSchema : BackupCheck
    data object Invalid : BackupCheck
}

/**
 * Faithful port of settings.js's Backup & Restore (§8.6): serializes the
 * entire local storage (all DataStore prefs, saved playlists, and explicit
 * recommendation exclusions) into one JSON file, and restores it with a
 * pre-restore snapshot so any failure mid-apply rolls back automatically
 * rather than leaving a half-restored state.
 *
 * Older backups remain compatible; their former automatic history is
 * intentionally ignored and never converted into explicit dislikes.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val playlistDao: SavedPlaylistDao,
    private val recommendationExclusionDao: RecommendationExclusionDao,
    private val playlistPublicMirror: PlaylistPublicMirror,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    fun checkBackup(content: String): BackupCheck {
        val backup = runCatching { json.decodeFromString<BackupFile>(content) }.getOrNull()
            ?: return BackupCheck.Invalid
        if (backup.type != BACKUP_TYPE) return BackupCheck.Invalid
        if (backup.schemaVersion > SCHEMA_VERSION) return BackupCheck.UnsupportedSchema
        return BackupCheck.Valid(backup.playlists.size)
    }

    suspend fun buildBackup(appVersionName: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prefs = dataStore.data.first()
        val strings = mutableMapOf<String, String>()
        val booleans = mutableMapOf<String, Boolean>()
        val integers = mutableMapOf<String, Int>()
        val longs = mutableMapOf<String, Long>()
        val stringSets = mutableMapOf<String, Set<String>>()
        val floats = mutableMapOf<String, Float>()
        for (entry in prefs.asMap()) {
            val key = entry.key.name
            if (key in SENSITIVE_PREFERENCE_NAMES) continue
            when (val value = entry.value) {
                is String -> strings[key] = value
                is Boolean -> booleans[key] = value
                // A timestamp found as Int has already been truncated by the
                // old schema and cannot be reconstructed; reset it safely.
                is Int -> if (key in LONG_PREFERENCE_NAMES) longs[key] = 0L else integers[key] = value
                is Long -> longs[key] = value
                is Float -> floats[key] = value
                is Set<*> -> stringSets[key] = value.mapNotNull { it as? String }.toSet()
                else -> Unit
            }
        }
        val playlists = playlistDao.getAll().map {
            BackupPlaylistSnapshot(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
                mode = it.mode,
                tracksJson = it.tracksJson,
                createdAtMillis = it.createdAtMillis,
                discoverSignature = it.discoverSignature,
                customCoverUri = it.customCoverUri,
                isPinned = it.isPinned,
            )
        }
        val exclusions = recommendationExclusionDao.getAll().map {
            BackupRecommendationExclusionSnapshot(
                trackKey = it.trackKey,
                excludedAtMillis = it.excludedAtMillis,
                trackName = it.trackName,
                artistName = it.artistName,
            )
        }
        val backup = BackupFile(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersionName,
            prefs = BackupPrefsSnapshot(
                strings = strings,
                booleans = booleans,
                integers = integers,
                longs = longs,
                stringSets = stringSets,
                floats = floats,
            ),
            playlists = playlists,
            recommendationExclusions = exclusions,
        )
        json.encodeToString(backup)
    }

    suspend fun restore(
        content: String,
        preserveSignedInSession: Boolean = false,
    ): RestoreResult {
        val backup = try {
            json.decodeFromString<BackupFile>(content)
        } catch (e: Exception) {
            return RestoreResult.InvalidFile
        }
        if (backup.type != BACKUP_TYPE) return RestoreResult.InvalidFile
        if (backup.schemaVersion > SCHEMA_VERSION) return RestoreResult.UnsupportedSchema

        val currentPrefs = try {
            dataStore.data.first()
        } catch (error: Exception) {
            return RestoreResult.Failed(error.message ?: "Could not read current settings")
        }
        val preservedAuthStrings = if (preserveSignedInSession) {
            AUTH_PREFERENCE_NAMES.mapNotNull { name ->
                currentPrefs.readSafely(stringPreferencesKey(name))?.let { value -> name to value }
            }.toMap()
        } else {
            emptyMap()
        }

        // The rollback snapshot must capture the EXACT pre-restore state,
        // including the credentials buildBackup() deliberately strips from
        // exports — building it from buildBackup() silently destroyed the
        // signed-in session on any mid-restore failure. It is captured raw
        // from the prefs already read above, and restore aborts BEFORE
        // mutating anything when any part of the previous state cannot be
        // captured (a missing snapshot previously degraded rollback into a
        // partial wipe).
        val previousPrefsSnapshot = try {
            json.encodeToString(
                BackupFile(
                    createdAt = 0L,
                    appVersion = "rollback",
                    prefs = rawSnapshotOf(currentPrefs),
                    playlists = emptyList(),
                )
            )
        } catch (e: Exception) {
            return RestoreResult.Failed("Could not snapshot current settings: ${e.message}")
        }
        val previousPlaylists = try {
            playlistDao.getAll()
        } catch (e: Exception) {
            return RestoreResult.Failed("Could not snapshot current playlists: ${e.message}")
        }
        val previousExclusions = try {
            recommendationExclusionDao.getAll()
        } catch (e: Exception) {
            return RestoreResult.Failed("Could not snapshot current exclusions: ${e.message}")
        }

        return try {
            dataStore.edit { mutablePrefs ->
                mutablePrefs.clear()
                // A backup file is untrusted input: credential keys are never
                // accepted from it, even if a crafted file contains them.
                // Sessions are only ever restored from the device's own
                // current values (preservedAuthStrings below), never from the
                // file being imported.
                backup.prefs.strings.forEach { (k, v) ->
                    if (k !in SENSITIVE_PREFERENCE_NAMES) mutablePrefs[stringPreferencesKey(k)] = v
                }
                // The sensitive-key skip applies to every typed map, not
                // just strings: a crafted backup could otherwise place a
                // credential name in a wrongly-typed bucket.
                backup.prefs.booleans.forEach { (k, v) ->
                    if (k !in SENSITIVE_PREFERENCE_NAMES) mutablePrefs[booleanPreferencesKey(k)] = v
                }
                backup.prefs.integers.forEach { (k, v) ->
                    if (k in SENSITIVE_PREFERENCE_NAMES) return@forEach
                    // Repair v7-and-older backups that wrote Long preferences
                    // into the integer bucket.
                    if (k in LONG_PREFERENCE_NAMES) {
                        mutablePrefs[longPreferencesKey(k)] = 0L
                    } else {
                        mutablePrefs[intPreferencesKey(k)] = v
                    }
                }
                backup.prefs.longs.forEach { (k, v) ->
                    if (k !in SENSITIVE_PREFERENCE_NAMES) mutablePrefs[longPreferencesKey(k)] = v
                }
                backup.prefs.stringSets.forEach { (k, v) ->
                    if (k !in SENSITIVE_PREFERENCE_NAMES) mutablePrefs[stringSetPreferencesKey(k)] = v
                }
                backup.prefs.floats.forEach { (k, v) ->
                    if (k !in SENSITIVE_PREFERENCE_NAMES) mutablePrefs[floatPreferencesKey(k)] = v
                }
                if (preserveSignedInSession) {
                    preservedAuthStrings.forEach { (name, value) ->
                        mutablePrefs[stringPreferencesKey(name)] = value
                    }
                    mutablePrefs[booleanPreferencesKey(GUEST_MODE_PREFERENCE)] = false
                }
            }
            playlistDao.replaceAll(backup.playlists.map { p ->
                SavedPlaylistEntity(
                    id = p.id,
                    title = p.title,
                    subtitle = p.subtitle,
                    mode = p.mode,
                    tracksJson = p.tracksJson,
                    createdAtMillis = p.createdAtMillis,
                    discoverSignature = p.discoverSignature,
                    customCoverUri = p.customCoverUri,
                    isPinned = p.isPinned,
                )
            })
            if (backup.schemaVersion >= 7) {
                recommendationExclusionDao.clear()
                recommendationExclusionDao.upsertAll(
                    backup.recommendationExclusions.map {
                        RecommendationExclusionEntity(
                            trackKey = it.trackKey,
                            excludedAtMillis = it.excludedAtMillis,
                            trackName = it.trackName,
                            artistName = it.artistName,
                        )
                    },
                )
            }
            playlistPublicMirror.writeFromDatabase()
            RestoreResult.Success(backup.playlists.size, backup.recommendationExclusions.size)
        } catch (e: Exception) {
            try {
                rollback(previousPrefsSnapshot)
                if (preserveSignedInSession) {
                    // rollback() restores the raw snapshot (which already
                    // contains these), but re-apply defensively in case the
                    // snapshot predates a concurrent sign-in.
                    dataStore.edit { mutablePrefs ->
                        preservedAuthStrings.forEach { (name, value) ->
                            mutablePrefs[stringPreferencesKey(name)] = value
                        }
                    }
                }
                playlistDao.replaceAll(previousPlaylists)
                recommendationExclusionDao.clear()
                recommendationExclusionDao.upsertAll(previousExclusions)
            } catch (rollbackError: Exception) {
                // Nothing more we can safely do — surface the original failure.
            }
            RestoreResult.Failed(e.message ?: "Restore failed")
        }
    }

    /** Raw, unfiltered snapshot of [prefs] for rollback use only — never exported. */
    private fun rawSnapshotOf(prefs: Preferences): BackupPrefsSnapshot {
        val strings = mutableMapOf<String, String>()
        val booleans = mutableMapOf<String, Boolean>()
        val integers = mutableMapOf<String, Int>()
        val longs = mutableMapOf<String, Long>()
        val stringSets = mutableMapOf<String, Set<String>>()
        val floats = mutableMapOf<String, Float>()
        for (entry in prefs.asMap()) {
            val key = entry.key.name
            when (val value = entry.value) {
                is String -> strings[key] = value
                is Boolean -> booleans[key] = value
                is Int -> if (key in LONG_PREFERENCE_NAMES) longs[key] = 0L else integers[key] = value
                is Long -> longs[key] = value
                is Float -> floats[key] = value
                is Set<*> -> stringSets[key] = value.mapNotNull { it as? String }.toSet()
                else -> Unit
            }
        }
        return BackupPrefsSnapshot(
            strings = strings,
            booleans = booleans,
            integers = integers,
            longs = longs,
            stringSets = stringSets,
            floats = floats,
        )
    }

    private suspend fun rollback(snapshotJson: String) {
        val snapshot = json.decodeFromString<BackupFile>(snapshotJson)
        dataStore.edit { mutablePrefs ->
            mutablePrefs.clear()
            snapshot.prefs.strings.forEach { (k, v) -> mutablePrefs[stringPreferencesKey(k)] = v }
            snapshot.prefs.booleans.forEach { (k, v) -> mutablePrefs[booleanPreferencesKey(k)] = v }
            snapshot.prefs.integers.forEach { (k, v) ->
                if (k in LONG_PREFERENCE_NAMES) {
                    mutablePrefs[longPreferencesKey(k)] = 0L
                } else {
                    mutablePrefs[intPreferencesKey(k)] = v
                }
            }
            snapshot.prefs.longs.forEach { (k, v) -> mutablePrefs[longPreferencesKey(k)] = v }
            snapshot.prefs.stringSets.forEach { (k, v) -> mutablePrefs[stringSetPreferencesKey(k)] = v }
            snapshot.prefs.floats.forEach { (k, v) -> mutablePrefs[floatPreferencesKey(k)] = v }
        }
    }

    private companion object {
        const val GUEST_MODE_PREFERENCE = "lw_guest_mode"
        val AUTH_PREFERENCE_NAMES = listOf(
            "lw_apikey",
            "lw_apisecret",
            "lw_sessionkey",
            "lw_username",
            // The YouTube session is part of the signed-in session too; it was
            // previously missing here, so preserveSignedInSession never
            // actually protected it even on the success path.
            "ytm_cookies",
        )
    }
}
