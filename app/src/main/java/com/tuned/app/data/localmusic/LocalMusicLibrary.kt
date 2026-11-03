package com.tuned.app.data.localmusic

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.generate.LocalAudioFormats
import com.tuned.app.data.playlist.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature "Local songs": finds audio on this device and lists it in the
 * built-in "Local songs" playlist. Songs are played IN PLACE — nothing is
 * copied:
 *
 * - Picked folders (Storage Access Framework trees): each track is a
 *   `content://` document reference under the folder's persisted read
 *   grant. Tags and embedded covers are read from the original file; a
 *   sidecar `.lrc` next to it is small, so its text is stashed in
 *   [LocalLyricsStore] for the player.
 * - Device scan (MediaStore): one audio permission finds every music
 *   file on the device, whatever folder it lives in; tracks are
 *   `content://media/…` references with MediaStore's own metadata.
 *
 * Older versions COPIED picked files into app-private storage. Those
 * copies (and playlist rows pointing at them) keep working untouched;
 * [cleanupOrphans] still removes a copy once no playlist references it.
 *
 * Re-scans are incremental: a track is identified by its reference, and
 * a (size, last-modified) fingerprint decides whether tags are re-read.
 */
@Singleton
class LocalMusicLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
) {
    data class ImportReport(
        val added: Int = 0,
        val updated: Int = 0,
        val unchanged: Int = 0,
        /** Extension -> count of audio files this player cannot open. */
        val unsupported: Map<String, Int> = emptyMap(),
        val failed: Int = 0,
        /** Always 0 now: audio is referenced in place, never copied. */
        val bytesCopied: Long = 0L,
        val unreachableFolders: Int = 0,
    )

    sealed interface ImportState {
        data object Idle : ImportState
        data class Scanning(val filesFound: Int) : ImportState
        data class Importing(val done: Int, val total: Int, val currentName: String) : ImportState
        data class Finished(val report: ImportReport) : ImportState
    }

    private data class AudioEntry(
        val treeUri: Uri,
        val documentId: String,
        val displayName: String,
        val size: Long,
        val lastModified: Long,
        val lrcDocumentId: String?,
        val coverDocumentId: String?,
        /** The folder holding the file, the album when the tags don't name one. */
        val folderName: String?,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var job: Job? = null
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** Legacy copies root (pre-redesign imports). New scans never write audio here. */
    private val root: File get() = File(context.filesDir, DIR).apply { mkdirs() }
    private val artDir: File get() = File(root, ART_DIR).apply { mkdirs() }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _folders = MutableStateFlow(loadFolders())
    val folders: StateFlow<List<Uri>> = _folders.asStateFlow()
    private val _trackFolders = MutableStateFlow(loadTrackFolders())
    /** Which picked folder each song came from: its track url to the folder's address. */
    val trackFolders: StateFlow<Map<String, String>> = _trackFolders.asStateFlow()
    private val _trackDirs = MutableStateFlow(loadTrackDirs())
    /** Device-scan songs only: track url to its folder's relative path ("Music/Download/"). */
    val trackDirs: StateFlow<Map<String, String>> = _trackDirs.asStateFlow()

    val isRunning: Boolean get() = job?.isActive == true

    /** Persists read access to [treeUri], remembers it and imports it. */
    fun addFolderAndImport(treeUri: Uri) {
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { Log.w(TAG, "No persistable grant for $treeUri", it) }.isSuccess
        if (!granted) {
            // Without the persisted grant the folder would silently vanish
            // at the next rescan/reboot. Don't save it at all, and tell the
            // user through the panel's existing "folder out of reach —
            // choose it again" report instead of only logging it.
            if (!isRunning) _state.value = ImportState.Finished(ImportReport(unreachableFolders = 1))
            return
        }
        val updated = (_folders.value.filterNot { it == treeUri } + treeUri)
        saveFolders(updated)
        launchImport(listOf(treeUri))
    }

    /** Re-imports every remembered folder, plus the device scan when it was enabled before. */
    fun rescanAll() {
        if (deviceScanEnabled() && hasDeviceScanPermission()) {
            launchJob {
                val folderReport = runImport(_folders.value)
                val deviceReport = runDeviceScan()
                mergeReports(folderReport, deviceReport)
            }
        } else {
            launchImport(_folders.value)
        }
    }

    /**
     * Forgets a folder (releases the grant). Reference tracks from this
     * folder are removed from "Local songs" — without the grant they can
     * no longer be played. Legacy copied songs are self-contained files
     * and stay, exactly as before.
     */
    fun forgetFolder(treeUri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        saveFolders(_folders.value.filterNot { it == treeUri })
        val folderKey = treeUri.toString()
        val referenceUrls = _trackFolders.value
            .filterValues { it == folderKey }
            .keys
            .filter { it.startsWith("content://") }
            .toSet()
        saveTrackFolders(_trackFolders.value.filterValues { it != folderKey })
        if (referenceUrls.isNotEmpty()) {
            scope.launch {
                mutex.withLock {
                    runCatching {
                        playlistRepository.removeLocalSongsByUrls(referenceUrls)
                        referenceUrls.forEach { LocalLyricsStore.remove(context, it) }
                        saveFingerprints(loadFingerprints().filterKeys { it !in referenceUrls })
                    }.onFailure { Log.w(TAG, "Forget-folder cleanup failed", it) }
                }
            }
        }
    }

    /** Songs imported before folders were recorded get theirs the next time this is asked. */
    fun fillMissingTrackFolders() {
        if (_folders.value.isNotEmpty() && _trackFolders.value.isEmpty()) rescanAll()
    }

    // -- Device-wide MediaStore scan --------------------------------------

    /** The runtime permission a device scan needs on this Android version. */
    fun requiredAudioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasDeviceScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED

    fun deviceScanEnabled(): Boolean = prefs.getBoolean(KEY_DEVICE_SCAN, false)

    /** Scans the whole device via MediaStore and syncs "Local songs". */
    fun scanDevice() {
        if (!hasDeviceScanPermission()) return
        launchJob { runDeviceScan() }
    }

    // -- Auto-discovery -----------------------------------------------------

    private var observerRegistered = false
    private var pendingAutoScan: Job? = null

    /**
     * Watches MediaStore for audio changes (a finished download, a file
     * copied over USB, a deletion) and silently re-scans when the device
     * scan is enabled — new songs simply appear in "Local songs", the way
     * they do in other local players. Registered once per process; the
     * observer lives as long as the app process, like the library itself.
     */
    fun startAutoDiscovery() {
        if (observerRegistered) return
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = scheduleAutoScan()
            override fun onChange(selfChange: Boolean, uri: Uri?) = scheduleAutoScan()
        }
        // Each registration stands on its own: the external volume is the
        // one that matters, and a failure there leaves the flag unset so
        // a later start can retry.
        val externalOk = runCatching {
            context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        }.onFailure { Log.w(TAG, "Auto-discovery observer not registered (external)", it) }.isSuccess
        runCatching {
            context.contentResolver.registerContentObserver(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, observer)
        }.onFailure { Log.w(TAG, "Auto-discovery observer not registered (internal)", it) }
        if (externalOk) observerRegistered = true
    }

    private fun scheduleAutoScan() {
        if (!deviceScanEnabled() || !hasDeviceScanPermission()) return
        pendingAutoScan?.cancel()
        pendingAutoScan = scope.launch {
            // A download manager writing ten files fires ten changes;
            // wait for the burst to settle, then scan once.
            kotlinx.coroutines.delay(AUTO_SCAN_DEBOUNCE_MS)
            runSilentDeviceScan()
        }
    }

    private var lastBackgroundScanMs = 0L

    /**
     * Silent rescan used when the Local section opens. Throttled: the
     * panel leaves and re-enters composition as it scrolls off and back,
     * and each visit must not cost a full MediaStore query. When another
     * scan is already running this queues behind it on the mutex rather
     * than dropping the refresh.
     */
    fun refreshDeviceInBackground() {
        if (!deviceScanEnabled() || !hasDeviceScanPermission()) return
        val now = System.currentTimeMillis()
        if (now - lastBackgroundScanMs < BACKGROUND_REFRESH_MIN_INTERVAL_MS) return
        lastBackgroundScanMs = now
        scope.launch {
            mutex.withLock {
                runCatching { runDeviceScan(silent = true) }
                    .onFailure {
                        if (it is CancellationException) throw it
                        Log.w(TAG, "Background device scan failed", it)
                    }
            }
        }
    }

    private suspend fun runSilentDeviceScan() {
        // No isRunning early-out: the mutex serializes behind any manual
        // scan, and a device change that lands during one must not be lost.
        mutex.withLock {
            runCatching { runDeviceScan(silent = true) }
                .onFailure {
                    if (it is CancellationException) throw it
                    Log.w(TAG, "Auto device scan failed", it)
                }
        }
    }

    private suspend fun runDeviceScan(silent: Boolean = false): ImportReport {
        if (!silent) _state.value = ImportState.Scanning(0)
        val tracks = mutableListOf<GeneratedTrack>()
        val dirs = mutableMapOf<String, String>()
        var failed = 0
        val safPaths = safReferencedPaths()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $MIN_DEVICE_TRACK_MS"
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val relCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                while (c.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val data = c.getString(dataCol)?.takeIf { it.isNotBlank() }
                    // The user picked this folder explicitly; its SAF entry
                    // already covers the file — don't list it twice.
                    if (data != null && data in safPaths) continue
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val url = uri.toString()
                    val displayName = c.getString(nameCol).orEmpty()
                    val title = c.getString(titleCol)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: LocalMusicNaming.fromFileName(displayName).title
                    val rawArtist = c.getString(artistCol)?.trim()
                    val artist = rawArtist?.takeIf { it.isNotEmpty() && !it.equals(UNKNOWN_TAG, ignoreCase = true) }
                        ?: LocalMusicNaming.fromFileName(displayName).artist
                        ?: UNKNOWN_ARTIST
                    val album = c.getString(albumCol)?.trim()
                        ?.takeIf { it.isNotEmpty() && !it.equals(UNKNOWN_TAG, ignoreCase = true) }
                    val albumId = c.getLong(albumIdCol)
                    val artwork = albumId.takeIf { it > 0 }?.let {
                        ContentUris.withAppendedId(ALBUM_ART_URI, it).toString()
                    }
                    tracks += GeneratedTrack(
                        name = title,
                        artist = artist,
                        album = album,
                        artworkUrl = artwork,
                        url = url,
                    )
                    // Which folder the file lives in, for the Folders
                    // category: RELATIVE_PATH ("Music/Download/") first,
                    // the DATA parent as fallback.
                    val relDir = c.getString(relCol)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: data?.let { relativeDirOf(it) }
                    if (relDir != null) dirs[url] = relDir
                    // A sidecar .lrc beside the real file: stash its text
                    // (kilobytes) so in-place playback still finds lyrics.
                    if (data != null) {
                        runCatching {
                            val lrc = File(LocalAudioFormats.lrcPathFor(data))
                            if (lrc.isFile && lrc.length() in 1..MAX_LRC_BYTES) {
                                LocalLyricsStore.put(context, url, lrc.readText())
                            }
                        }
                    }
                    if (!silent) _state.value = ImportState.Scanning(tracks.size)
                }
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Log.w(TAG, "Device scan failed", it)
            failed++
        }
        if (failed > 0) return ImportReport(failed = failed)
        prefs.edit().putBoolean(KEY_DEVICE_SCAN, true).apply()
        val (added, updated, _) = playlistRepository.syncLocalSongsByPrefix(MEDIASTORE_URL_PREFIX, tracks)
        // Refresh the folder map: this scan's entries replace the old
        // MediaStore ones; picked-folder entries are a separate map.
        saveTrackDirs(_trackDirs.value.filterKeys { !it.startsWith(MEDIASTORE_URL_PREFIX) } + dirs)
        pruneLyrics()
        return ImportReport(added = added, updated = updated, unchanged = tracks.size - added - updated)
    }

    /**
     * "Music/Download/" for "/storage/emulated/0/Music/Download/song.mp3".
     * The primary volume's root is TWO segments ("emulated/0"); SD-card
     * volumes mount under a single-segment id ("/storage/1234-5678/…").
     * A file at a volume root has no folder and returns null.
     */
    private fun relativeDirOf(dataPath: String): String? {
        if (!dataPath.startsWith("/storage/")) return null
        val rest = dataPath.removePrefix("/storage/")
        val relative = if (rest.startsWith("emulated/")) {
            rest.removePrefix("emulated/").substringAfter('/', "")
        } else {
            rest.substringAfter('/', "")
        }
        val parent = relative.substringBeforeLast('/', "")
        return parent.takeIf { it.isNotEmpty() }?.let { "$it/" }
    }

    /** Real file paths already covered by picked-folder (SAF) references. */
    private suspend fun safReferencedPaths(): Set<String> {
        val local = playlistRepository.getLocalSongs()?.tracks.orEmpty()
        return local.mapNotNullTo(mutableSetOf()) { track ->
            val url = track.url
            if (!url.startsWith("content://") || !url.contains("/document/")) return@mapNotNullTo null
            val docId = runCatching { DocumentsContract.getDocumentId(Uri.parse(url)) }.getOrNull()
                ?: return@mapNotNullTo null
            val volume = docId.substringBefore(':')
            val path = docId.substringAfter(':', "")
            if (path.isEmpty()) return@mapNotNullTo null
            // Primary storage mounts at /storage/emulated/0; every other
            // volume (SD cards) mounts under its own volume id.
            val base = if (volume == "primary") "/storage/emulated/0" else "/storage/$volume"
            "$base/$path"
        }
    }

    private fun mergeReports(a: ImportReport, b: ImportReport): ImportReport = ImportReport(
        added = a.added + b.added,
        updated = a.updated + b.updated,
        unchanged = a.unchanged + b.unchanged,
        unsupported = (a.unsupported.keys + b.unsupported.keys).associateWith { k ->
            (a.unsupported[k] ?: 0) + (b.unsupported[k] ?: 0)
        },
        failed = a.failed + b.failed,
        unreachableFolders = a.unreachableFolders + b.unreachableFolders,
    )

    private suspend fun pruneLyrics() {
        runCatching {
            val playlists = playlistRepository.getAll()
            // Same caution as cleanupOrphans: an empty read means storage
            // failed; pruning against it would delete every sidecar.
            if (playlists.isEmpty()) return
            // Liveness spans ALL playlists: a reference track can live in a
            // custom playlist after leaving "Local songs".
            val live = playlists.flatMap { it.tracks }.mapTo(mutableSetOf()) { it.url }
            LocalLyricsStore.prune(context, live)
        }
    }

    // -- Shared preference-backed bookkeeping ------------------------------

    /**
     * Map persistence, format v2: keys and values are Uri-encoded before
     * joining, so a tab or newline inside a folder path can no longer split
     * one entry into two corrupt ones (the v1 first-tab split silently
     * dropped such tracks into the "unknown folder" group). v1 data (no
     * header line) is still read with the old split, exactly as written.
     */
    private fun encodeMap(map: Map<String, String>): String =
        MAP_FORMAT_V2 + map.entries.joinToString("\n") { "${Uri.encode(it.key)}\t${Uri.encode(it.value)}" }

    private fun decodeMap(raw: String?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val lines = raw.lineSequence().toList()
        val v2 = lines.firstOrNull() == MAP_FORMAT_V2.trimEnd()
        return lines.drop(if (v2) 1 else 0)
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val key = line.substring(0, tab)
                val value = line.substring(tab + 1)
                if (v2) Uri.decode(key) to Uri.decode(value) else key to value
            }
            .toMap()
    }

    private fun loadTrackFolders(): Map<String, String> = decodeMap(prefs.getString(KEY_TRACK_FOLDERS, null))

    private fun saveTrackFolders(map: Map<String, String>) {
        prefs.edit().putString(KEY_TRACK_FOLDERS, encodeMap(map)).apply()
        _trackFolders.value = map
    }

    private fun loadTrackDirs(): Map<String, String> = decodeMap(prefs.getString(KEY_TRACK_DIRS, null))

    private fun saveTrackDirs(map: Map<String, String>) {
        prefs.edit().putString(KEY_TRACK_DIRS, encodeMap(map)).apply()
        _trackDirs.value = map
    }

    private fun loadFingerprints(): Map<String, String> = decodeMap(prefs.getString(KEY_FINGERPRINTS, null))

    private fun saveFingerprints(map: Map<String, String>) {
        prefs.edit().putString(KEY_FINGERPRINTS, encodeMap(map)).apply()
    }

    fun acknowledgeReport() {
        if (_state.value is ImportState.Finished) _state.value = ImportState.Idle
    }

    fun folderLabel(treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return treeUri.toString()
        // "primary:Music/Albums" -> "Music/Albums"; SD cards keep their volume id.
        val (volume, path) = docId.substringBefore(':') to docId.substringAfter(':', "")
        return when {
            path.isBlank() -> if (volume == "primary") "Internal storage" else volume
            volume == "primary" -> path
            else -> "$volume/$path"
        }
    }

    // -- Picked-folder (SAF) import ----------------------------------------

    private fun launchImport(folders: List<Uri>) {
        if (folders.isEmpty()) return
        launchJob { runImport(folders) }
    }

    private fun launchJob(block: suspend () -> ImportReport) {
        if (isRunning) return
        job = scope.launch {
            mutex.withLock {
                _state.value = ImportState.Scanning(0)
                val report = try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Local import failed", e)
                    ImportReport(failed = 1)
                }
                _state.value = ImportState.Finished(report)
            }
        }
    }

    private suspend fun runImport(folders: List<Uri>): ImportReport {
        val entries = mutableListOf<AudioEntry>()
        val unsupported = mutableMapOf<String, Int>()
        var unreachable = 0
        // Per-folder seen sets, for deletion sync below: only folders whose
        // walk SUCCEEDED take part — an unreachable folder must never read
        // as "all its files were deleted".
        val seenByFolder = mutableMapOf<String, Set<String>>()
        for (tree in folders) {
            val start = entries.size
            val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
            val walked = rootDocId != null &&
                walk(tree, rootDocId, rootDocId.substringAfterLast(':').substringAfterLast('/'), 0, entries, unsupported)
            if (!walked) {
                unreachable++
            } else {
                seenByFolder[tree.toString()] = entries.subList(start, entries.size)
                    .mapTo(mutableSetOf()) {
                        DocumentsContract.buildDocumentUriUsingTree(it.treeUri, it.documentId).toString()
                    }
            }
        }

        val existing = playlistRepository.ensureLocalSongs().tracks.associateBy { it.url }
        val fingerprints = loadFingerprints().toMutableMap()
        val updates = mutableMapOf<String, GeneratedTrack>()
        val additions = mutableListOf<GeneratedTrack>()
        var unchanged = 0
        var failed = 0
        var migrated = 0
        val migratedLegacy = mutableSetOf<String>()
        val folderOf = _trackFolders.value.toMutableMap()
        entries.forEachIndexed { index, entry ->
            currentCoroutineContext().ensureActive()
            _state.value = ImportState.Importing(index, entries.size, entry.displayName)
            try {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(entry.treeUri, entry.documentId)
                val url = docUri.toString()
                folderOf[url] = entry.treeUri.toString()
                // Upgrade migration: the pre-redesign importer stored this
                // exact document as a copied file named
                // stableId("tree|documentId").ext. When that legacy row is
                // still in "Local songs", the in-place reference built
                // below replaces it (the row is removed after the merge),
                // so a rescan never lists the same song twice. The copy
                // file itself is left for orphan cleanup — another
                // playlist may still point at it.
                val legacyPath = File(
                    root,
                    LocalMusicNaming.stableId("${entry.treeUri}|${entry.documentId}") +
                        "." + LocalAudioFormats.extensionOf(entry.displayName),
                ).absolutePath
                if (legacyPath in existing) migratedLegacy += legacyPath
                val fingerprint = "${entry.size}:${entry.lastModified}"
                val known = existing[url]
                if (known != null && fingerprints[url] == fingerprint) {
                    unchanged++
                    // Lyrics may have appeared beside the file since.
                    if (entry.lrcDocumentId != null && LocalLyricsStore.pathIfExists(context, url) == null) {
                        stashLyrics(entry, url)
                    }
                } else {
                    val track = readTrack(docUri, entry)
                    if (entry.lrcDocumentId != null) stashLyrics(entry, url)
                    fingerprints[url] = fingerprint
                    if (known != null) {
                        updates[url] = track
                    } else {
                        additions += track
                        if (legacyPath in migratedLegacy) migrated++
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't import ${entry.displayName}", e)
                failed++
            }
        }
        _state.value = ImportState.Importing(entries.size, entries.size, "")
        // Deletion sync (picked folders): a reference whose file is gone
        // from a successfully walked folder leaves "Local songs", with its
        // stashed lyrics and fingerprint — the device scan already syncs
        // this way via prefix replacement; the SAF path used to only ever
        // add, so deleted files stayed listed (and unplayable) forever.
        val staleUrls = mutableSetOf<String>()
        for ((treeStr, seen) in seenByFolder) {
            for ((url, folder) in _trackFolders.value) {
                if (folder == treeStr && url.startsWith("content://") &&
                    url !in seen && url !in migratedLegacy
                ) {
                    staleUrls += url
                }
            }
        }
        if (staleUrls.isNotEmpty()) {
            playlistRepository.removeLocalSongsByUrls(staleUrls)
            staleUrls.forEach { url ->
                LocalLyricsStore.remove(context, url)
                fingerprints.remove(url)
                folderOf.remove(url)
            }
            Log.i(TAG, "Rescan removed ${staleUrls.size} songs deleted from picked folders")
        }
        migratedLegacy.forEach { folderOf.remove(it) }
        saveTrackFolders(folderOf)
        saveFingerprints(fingerprints)
        if (updates.isNotEmpty() || additions.isNotEmpty()) {
            playlistRepository.mergeLocalSongs(updates, additions)
        }
        if (migratedLegacy.isNotEmpty()) {
            playlistRepository.removeLocalSongsByUrls(migratedLegacy)
        }
        pruneLyrics()
        return ImportReport(
            added = additions.size - migrated,
            updated = updates.size + migrated,
            unchanged = unchanged,
            unsupported = unsupported,
            failed = failed,
            unreachableFolders = unreachable,
        )
    }

    /** Reads a sidecar `.lrc` document's text into the lyrics store. */
    private fun stashLyrics(entry: AudioEntry, trackUrl: String) {
        runCatching {
            val lrcId = entry.lrcDocumentId ?: return
            val lrcUri = DocumentsContract.buildDocumentUriUsingTree(entry.treeUri, lrcId)
            val text = context.contentResolver.openInputStream(lrcUri)?.use { input ->
                // Bounded read: a sidecar is kilobytes; never load an
                // unbounded stream into memory to check its size after.
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0
                var overCap = false
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_LRC_BYTES) { overCap = true; break }
                    out.write(buffer, 0, read)
                }
                if (overCap) null else String(out.toByteArray(), Charsets.UTF_8)
            }
            if (text != null) LocalLyricsStore.put(context, trackUrl, text)
        }.onFailure { Log.w(TAG, "Couldn't read lyrics for ${entry.displayName}", it) }
    }

    /** Depth-first walk of a SAF tree. Returns false when the folder can't be listed. */
    private suspend fun walk(
        tree: Uri,
        parentDocId: String,
        folderName: String,
        depth: Int,
        out: MutableList<AudioEntry>,
        unsupported: MutableMap<String, Int>,
    ): Boolean {
        currentCoroutineContext().ensureActive()
        if (depth > MAX_DEPTH) return true
        data class Child(val id: String, val name: String, val mime: String?, val size: Long, val lastModified: Long)
        val children = mutableListOf<Child>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
        fun queryChildren(projection: Array<String>, withModified: Boolean): Boolean {
            children.clear()
            return runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        if (id != null) {
                            children += Child(
                                id = id,
                                name = c.getString(1).orEmpty(),
                                mime = c.getString(2),
                                size = if (c.isNull(3)) -1L else c.getLong(3),
                                lastModified = if (withModified && !c.isNull(4)) c.getLong(4) else 0L,
                            )
                        }
                    }
                    true
                } ?: false
            }.getOrDefault(false)
        }
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        // A third-party documents provider may reject the LAST_MODIFIED
        // column outright; fall back to the long-standing four columns
        // (lastModified 0 — fingerprints self-heal if it appears later)
        // instead of reporting the whole folder unreachable.
        val ok = queryChildren(projection, withModified = true) ||
            queryChildren(projection.copyOf(4), withModified = false)
        if (!ok) return false

        val files = children.filter { it.mime != DocumentsContract.Document.MIME_TYPE_DIR }
        val lrcByBase = files.filter { LocalAudioFormats.extensionOf(it.name) == "lrc" }
            .associateBy { it.name.substringBeforeLast('.').lowercase() }
        val folderCover = files.firstOrNull { file ->
            val base = file.name.substringBeforeLast('.').lowercase()
            val ext = LocalAudioFormats.extensionOf(file.name)
            base in COVER_NAMES && ext in IMAGE_EXTENSIONS
        }
        files.sortedBy { it.name.lowercase() }.forEach { file ->
            val ext = LocalAudioFormats.extensionOf(file.name)
            when {
                LocalAudioFormats.isSupported(file.name) -> out += AudioEntry(
                    treeUri = tree,
                    documentId = file.id,
                    displayName = file.name,
                    size = file.size,
                    lastModified = file.lastModified,
                    lrcDocumentId = lrcByBase[file.name.substringBeforeLast('.').lowercase()]?.id,
                    coverDocumentId = folderCover?.id,
                    folderName = folderName,
                )
                ext in LocalAudioFormats.UNSUPPORTED_AUDIO -> unsupported[ext] = (unsupported[ext] ?: 0) + 1
            }
        }
        _state.value = ImportState.Scanning(out.size)
        children.filter { it.mime == DocumentsContract.Document.MIME_TYPE_DIR }
            .sortedBy { it.name.lowercase() }
            .forEach { dir -> walk(tree, dir.id, dir.name, depth + 1, out, unsupported) }
        return true
    }

    /** Reads tags and cover from the ORIGINAL file, in place. */
    private fun readTrack(docUri: Uri, entry: AudioEntry): GeneratedTrack {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var trackNumber: Int? = null
        var year: String? = null
        var artwork: String? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, docUri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotEmpty() }
            artist = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                ?.trim()?.takeIf { it.isNotEmpty() }
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.takeIf { it.isNotEmpty() }
            trackNumber = LocalMusicNaming.trackNumber(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
            year = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE))
                ?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            retriever.embeddedPicture?.takeIf { it.isNotEmpty() }?.let { bytes ->
                val art = File(artDir, LocalMusicNaming.stableId(bytes) + ".img")
                if (!art.exists()) art.writeBytes(bytes)
                artwork = Uri.fromFile(art).toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No readable tags in ${entry.displayName}", e)
        } finally {
            runCatching { retriever.release() }
        }
        if (artwork == null && entry.coverDocumentId != null) {
            // The folder's cover image is referenced, not copied.
            artwork = DocumentsContract.buildDocumentUriUsingTree(entry.treeUri, entry.coverDocumentId).toString()
        }
        val fallback = LocalMusicNaming.fromFileName(entry.displayName)
        return GeneratedTrack(
            name = title ?: fallback.title,
            artist = artist ?: fallback.artist ?: UNKNOWN_ARTIST,
            album = album ?: entry.folderName?.let(LocalMusicNaming::albumFromFolder),
            trackNumber = trackNumber,
            year = year,
            artworkUrl = artwork,
            url = docUri.toString(),
        )
    }

    /**
     * Deletes legacy copied files no playlist references any more (after a
     * song was removed from "Local songs" and every other playlist, or the
     * playlist was deleted). Waits for a running import so freshly listed
     * files are safe. Reference tracks are never touched — there is no
     * copy to clean.
     */
    fun cleanupOrphansAsync() {
        scope.launch {
            mutex.withLock {
                runCatching { cleanupOrphans() }.onFailure { Log.w(TAG, "Orphan cleanup failed", it) }
            }
        }
    }

    private suspend fun cleanupOrphans() {
        val playlists = playlistRepository.getAll()
        // Liked Songs always exists: an empty read means storage failed, and
        // treating it as "nothing referenced" would wipe every copied song.
        if (playlists.isEmpty()) return
        val tracks = playlists.flatMap { it.tracks }
        val audio = tracks.mapNotNull { LocalAudioFormats.importedPathOrNull(it.url) }
            .mapTo(mutableSetOf()) { File(it).name }
        val art = tracks.mapNotNull { it.artworkUrl }
            .filter { it.startsWith("file://") }
            .mapTo(mutableSetOf()) { Uri.parse(it).lastPathSegment.orEmpty() }
        root.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val name = file.name
            val keep = when {
                name.endsWith(".part") -> false
                name.endsWith(".lrc") -> audio.any { it.substringBeforeLast('.') == name.substringBeforeLast('.') }
                else -> name in audio
            }
            if (!keep) file.delete()
        }
        artDir.listFiles()?.forEach { file -> if (file.name !in art) file.delete() }
    }

    /** Bytes used by legacy copies, extracted covers and stored lyrics. */
    fun storageBytes(): Long {
        var total = 0L
        if (root.exists()) total += root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val lyricsDir = File(context.filesDir, "local_lyrics")
        if (lyricsDir.exists()) total += lyricsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return total
    }

    private fun loadFolders(): List<Uri> =
        prefs.getString(KEY_FOLDERS, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?.map(Uri::parse)
            .orEmpty()

    private fun saveFolders(folders: List<Uri>) {
        prefs.edit().putString(KEY_FOLDERS, folders.joinToString("\n")).apply()
        _folders.value = folders
    }

    companion object {
        private const val TAG = "LocalMusicLibrary"
        const val DIR = LocalAudioFormats.IMPORT_DIR
        private const val ART_DIR = "art"
        private const val PREFS = "local_music"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_TRACK_FOLDERS = "track_folders"
        private const val KEY_FINGERPRINTS = "fingerprints"
        private const val KEY_DEVICE_SCAN = "device_scan_enabled"
        private const val KEY_TRACK_DIRS = "track_dirs"
        private const val AUTO_SCAN_DEBOUNCE_MS = 4_000L
        private const val BACKGROUND_REFRESH_MIN_INTERVAL_MS = 30_000L
        private const val MAX_DEPTH = 24
        private const val MEDIASTORE_URL_PREFIX = "content://media/"
        private const val MIN_DEVICE_TRACK_MS = 10_000
        private const val MAX_LRC_BYTES = 1024 * 1024
        private const val MAP_FORMAT_V2 = "v2\n"
        private const val UNKNOWN_TAG = "<unknown>"
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
        const val UNKNOWN_ARTIST = "Unknown artist"
        private val COVER_NAMES = setOf("cover", "folder", "front", "album", "albumart")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}

/** Pure helpers (unit-tested). */
object LocalMusicNaming {
    data class Names(val title: String, val artist: String?)

    /**
     * Fallback when a file has no tags: "01 - Artist - Title.flac" ->
     * (Title, Artist); "Artist - Title.mp3" -> (Title, Artist); else the
     * bare file name as title. A leading track number is dropped.
     */
    fun fromFileName(fileName: String): Names {
        val base = fileName.substringAfterLast('/').substringBeforeLast('.').trim()
        val withoutNumber = base.replace(LEADING_TRACK_NUMBER, "").trim().ifEmpty { base }
        val parts = withoutNumber.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            Names(title = parts.drop(1).joinToString(" - "), artist = parts.first())
        } else {
            Names(title = withoutNumber, artist = null)
        }
    }

    fun stableId(key: String): String = stableId(key.toByteArray(Charsets.UTF_8))

    fun stableId(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes)
            .joinToString("") { "%02x".format(it) }
            .take(24)

    /** A folder's name as an album, unless it's a general place to keep music. */
    fun albumFromFolder(name: String): String? =
        name.trim().takeIf { it.isNotEmpty() && it.lowercase() !in GENERIC_FOLDERS }

    /** The number from a track tag, which can read "3" or "3/12". */
    fun trackNumber(tag: String?): Int? = tag?.substringBefore('/')?.trim()?.toIntOrNull()

    private val GENERIC_FOLDERS = setOf(
        "music", "musik", "musique", "música", "audio", "songs", "tracks", "media", "mp3", "flac",
        "download", "downloads", "documents", "sdcard", "storage", "primary", "albums", "library",
        "unknown", "misc", "other", "new folder",
    )
    private val LEADING_TRACK_NUMBER = Regex("^\\d{1,3}\\s*[-._)]?\\s+")
}
