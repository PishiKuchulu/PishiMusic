package com.tuned.app.data

/**
 * Names of data saved on the device. These are the names already on people's phones;
 * changing one would orphan what users have (their library, settings, queue,
 * downloads), so they stay as they are. New stores should use Tuned's own names.
 */
object StorageNames {
    const val DATABASE = "lastwave.db"
    const val SETTINGS_STORE = "lastwave_prefs"
    const val WIDGET_STORE = "lastwave_widget_now_playing"
    const val EXCLUSIVE_USB_PREFS = "lastwave_exclusive_usb"
    const val BITPERFECT_VOLUME_PREFS = "lastwave_bitperfect_volume"
    const val PLAYBACK_SESSION_PREFS = "lastwave_playback_session"
    const val UPDATE_PREFS = "lastwave_updates"
    const val SEARCH_HISTORY_PREFS = "lastwave_search_history"
    const val LOCALE_FILE = "lastwave_locale_prefs"
    const val DOWNLOADS_CHANNEL = "lastwave_downloads"
    const val LEGACY_PLAYBACK_CHANNEL_V3 = "lastwave_playback_v3"
    const val LEGACY_PLAYBACK_CHANNEL_V2 = "lastwave_playback_v2"
    const val LEGACY_PLAYBACK_CHANNEL_V1 = "lastwave_playback"
    const val LEGACY_CRASH_LOG = "lastwave_crash_guard.log"
    const val LEGACY_BACKUP_TYPE = "lastwave-backup"
    const val PLAYLIST_MIRROR_TYPE = "lastwave-playlists"
    const val PLAYLIST_MIRROR_FILE = "lastwave-playlists.json"
    const val PLAYLIST_RECOVERY_FILE = "lastwave-playlists-recovery.json"
    const val LEGACY_DOWNLOAD_DIR = "LastWave"
    const val PLACEHOLDER_SCHEME = "lastwave"
    const val APPLE_LYRICS_ID = "lastwave"
}
