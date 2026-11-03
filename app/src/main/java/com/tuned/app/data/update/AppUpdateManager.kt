package com.tuned.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.tuned.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val downloadUrl: String? = null,
    val downloadFileName: String? = null,
    val isDismissed: Boolean = false,
    val message: String? = null,
    /** GitHub's published SHA-256 of the APK, when it gave one. */
    val apkSha256: String? = null,
    /** 0 to 1 while the APK is downloading, otherwise null. */
    val downloadProgress: Float? = null,
    val downloadFailed: Boolean = false,
    val apkReady: Boolean = false,
    /** Android needs the listener to allow installs from Tuned before it can go ahead. */
    val needsInstallPermission: Boolean = false,
)

/** Release notes are written in Markdown; this shows them as plain text. */
fun plainReleaseNotes(markdown: String): String = markdown
    .lineSequence()
    .map { line ->
        line.trim()
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^[-*]\\s+"), "• ")
            .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("[*`]"), "")
    }
    .joinToString("\n")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

/** The hash from a GitHub asset digest such as "sha256:ab12…", or null for anything else. */
fun sha256FromDigest(digest: String?): String? =
    digest?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()?.takeIf { it.length == 64 }

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sha256: String? = null,
)

enum class ApkVariant {
    UNCOMPRESSED,
    ANDROID7,
    UNIVERSAL,
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(com.tuned.app.data.StorageNames.UPDATE_PREFS, Context.MODE_PRIVATE)

    private val _updateInfo = MutableStateFlow(
        UpdateInfo(
            currentVersion = getCurrentVersion(),
        )
    )
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private var downloadJob: kotlinx.coroutines.Job? = null

    // Retained so cancelDownload() can abort the socket itself: cancelling
    // only the coroutine left a blocking read running, and the stale
    // coroutine could then rewrite UI state or rename the .part file back
    // into a "ready" APK after the user had cancelled.
    @Volatile
    private var activeDownloadCall: okhttp3.Call? = null

    @Volatile
    private var downloadGeneration: Int = 0

    init {
        // Whatever an earlier update left behind has done its job by now.
        scope.launch { runCatching { updatesDir().deleteRecursively() } }
        checkForUpdate(isSilent = true)
    }

    fun isUncompressedBuild(): Boolean {
        val pref = prefs.getString("preferred_variant", null)
        if (pref != null) {
            return pref.equals("uncompressed", ignoreCase = true) || pref.equals("raw", ignoreCase = true)
        }
        val buildType = runCatching { BuildConfig.BUILD_TYPE }.getOrDefault("")
        return buildType.equals("rawRelease", ignoreCase = true) ||
               buildType.contains("raw", ignoreCase = true) ||
               buildType.contains("uncompressed", ignoreCase = true)
    }

    fun getPreferredVariant(): String? = prefs.getString("preferred_variant", null)

    fun setPreferredVariant(variant: String?) {
        if (variant == null) {
            prefs.edit().remove("preferred_variant").apply()
        } else {
            prefs.edit().putString("preferred_variant", variant).apply()
        }
        checkForUpdate(isSilent = true)
    }

    fun getCurrentVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
    } catch (_: Exception) {
        BuildConfig.VERSION_NAME
    }

    fun checkForUpdate(isSilent: Boolean = false) {
        if (!isEnabled) return
        scope.launch {
            _updateInfo.update {
                it.copy(
                    isChecking = true,
                    message = if (!isSilent) "Checking for updates..." else it.message,
                )
            }
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$REPO/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Tuned-Android")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (!response.isSuccessful) {
                    val code = response.code
                    // The Response was never closed on this path (nor when
                    // parsing threw into the catch below): each failed check
                    // leaked a connection from the shared pool.
                    runCatching { response.close() }
                    _updateInfo.update {
                        it.copy(
                            isChecking = false,
                            message = if (!isSilent) "Could not check updates (HTTP $code)" else null,
                        )
                    }
                    return@launch
                }

                val body = response.body?.string().orEmpty()
                runCatching { response.close() }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val releaseUrl = json.optString("html_url", "https://github.com/$REPO/releases")
                val releaseNotes = json.optString("body", "")

                val apkAssets = mutableListOf<ReleaseAsset>()
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "").trim()
                        val download = asset.optString("browser_download_url", "").trim()
                        if (name.endsWith(".apk", ignoreCase = true) && download.isNotBlank()) {
                            apkAssets.add(
                                ReleaseAsset(
                                    name = name,
                                    downloadUrl = download,
                                    sha256 = sha256FromDigest(asset.optString("digest", "")),
                                ),
                            )
                        }
                    }
                }

                val bestAsset = selectBestAsset(
                    assets = apkAssets,
                    isUncompressed = isUncompressedBuild(),
                    sdkInt = Build.VERSION.SDK_INT,
                )
                // Fail closed on integrity: an APK asset WITHOUT a GitHub
                // digest cannot be hash-verified after download, so it is
                // never offered as an in-app install — the release page opens
                // in the browser instead.
                val verifiableAsset = bestAsset?.takeIf { it.sha256 != null }
                if (bestAsset != null && verifiableAsset == null) {
                    android.util.Log.w("AppUpdate", "Release asset ${bestAsset.name} has no digest; not offering in-app install")
                }
                val downloadUrl = verifiableAsset?.downloadUrl ?: releaseUrl
                val downloadFileName = verifiableAsset?.name

                val currentVersion = getCurrentVersion()
                val hasNewer = isNewerVersion(tagName, currentVersion)
                val cleanTag = tagName.removePrefix("v").removePrefix("V")
                val dismissedVersion = prefs.getString("dismissed_version", null)
                val isDismissed = dismissedVersion == cleanTag

                _updateInfo.update {
                    it.copy(
                        isChecking = false,
                        isUpdateAvailable = hasNewer,
                        latestVersion = cleanTag,
                        currentVersion = currentVersion,
                        releaseNotes = releaseNotes,
                        releaseUrl = releaseUrl,
                        downloadUrl = downloadUrl,
                        downloadFileName = downloadFileName,
                        apkSha256 = verifiableAsset?.sha256,
                        isDismissed = isDismissed,
                        message = if (!isSilent) {
                            if (hasNewer) "New version $cleanTag available!" else "You're on the latest version ($currentVersion)"
                        } else null,
                    )
                }
            } catch (e: Exception) {
                _updateInfo.update {
                    it.copy(
                        isChecking = false,
                        message = if (!isSilent) "Check failed: ${e.message ?: "Network error"}" else null,
                    )
                }
            }
        }
    }

    fun dismissUpdate(version: String) {
        val clean = version.removePrefix("v").removePrefix("V")
        prefs.edit().putString("dismissed_version", clean).apply()
        _updateInfo.update { it.copy(isDismissed = true) }
    }

    /** Downloads the APK and hands it to Android to install. A release without one opens in the browser. */
    fun startUpdate(context: Context) {
        val info = _updateInfo.value
        val url = info.downloadUrl
        val name = info.downloadFileName
        if (url == null || name == null) {
            openUpdate(context)
            return
        }
        if (downloadJob?.isActive == true) return
        val appContext = context.applicationContext
        val generation = ++downloadGeneration
        downloadJob = scope.launch {
            _updateInfo.update { it.copy(downloadProgress = 0f, downloadFailed = false, apkReady = false) }
            val ok = runCatching { downloadApk(url, info.apkSha256) }
                .onFailure { android.util.Log.w("AppUpdate", "Update download failed", it) }
                .getOrDefault(false)
            // A cancelled/superseded download must not touch state or launch
            // the installer after cancelDownload() already reset everything.
            if (generation != downloadGeneration) return@launch
            _updateInfo.update { it.copy(downloadProgress = null, downloadFailed = !ok, apkReady = ok) }
            if (ok) withContext(Dispatchers.Main) { install(appContext) }
        }
    }

    fun cancelDownload() {
        downloadGeneration++
        activeDownloadCall?.cancel()
        activeDownloadCall = null
        downloadJob?.cancel()
        downloadJob = null
        apkFile().delete()
        partFile().delete()
        _updateInfo.update { it.copy(downloadProgress = null, downloadFailed = false, apkReady = false) }
    }

    private fun updatesDir() = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
    private fun apkFile() = java.io.File(updatesDir(), "Tuned-update.apk")
    private fun partFile() = java.io.File(updatesDir(), "Tuned-update.apk.part")

    private suspend fun downloadApk(url: String, expectedSha256: String?): Boolean {
        val request = Request.Builder().url(url).header("User-Agent", "Tuned-Android").build()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val call = okHttpClient.newCall(request)
        activeDownloadCall = call
        try {
            return downloadApkBody(call, digest, expectedSha256)
        } finally {
            if (activeDownloadCall === call) activeDownloadCall = null
        }
    }

    private suspend fun downloadApkBody(
        call: okhttp3.Call,
        digest: java.security.MessageDigest,
        expectedSha256: String?,
    ): Boolean {
        call.execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val total = body.contentLength()
            var read = 0L
            var shownPercent = -1
            partFile().outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        out.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt()
                            if (percent != shownPercent) {
                                shownPercent = percent
                                _updateInfo.update { it.copy(downloadProgress = percent / 100f) }
                            }
                        }
                    }
                }
            }
        }
        // A damaged or swapped download must never reach the installer.
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (expectedSha256 != null && actual != expectedSha256) {
            android.util.Log.w("AppUpdate", "Update failed its hash check")
            partFile().delete()
            return false
        }
        apkFile().delete()
        return partFile().renameTo(apkFile())
    }

    /** Opens Android's installer on the downloaded APK, asking for permission to first if needed. */
    fun install(context: Context) {
        val apk = apkFile()
        if (!apk.exists()) {
            _updateInfo.update { it.copy(apkReady = false) }
            return
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            _updateInfo.update { it.copy(needsInstallPermission = true) }
            val settings = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return
        }
        _updateInfo.update { it.copy(needsInstallPermission = false) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { _updateInfo.update { state -> state.copy(downloadFailed = true) } }
    }

    fun openUpdate(context: Context) {
        val targetUrl = _updateInfo.value.downloadUrl ?: _updateInfo.value.releaseUrl.ifBlank {
            "https://github.com/$REPO/releases"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val cleanRemote = remote.removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanLocal = local.removePrefix("v").removePrefix("V").substringBefore("-")
        if (cleanRemote == cleanLocal) return false

        val rParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    companion object {
        /** GitHub "owner/repo" whose releases are checked, set at build time. */
        private val REPO: String = BuildConfig.UPDATE_GITHUB_REPO.trim()

        /** Update checks are off until a repository is configured. */
        val isEnabled: Boolean get() = REPO.isNotEmpty()

        fun classifyAsset(name: String): ApkVariant {
            val lower = name.lowercase()
            return when {
                lower.contains("uncompressed") || lower.contains("-raw") || lower.contains("_raw") -> ApkVariant.UNCOMPRESSED
                lower.contains("android7") || lower.contains("legacy") -> ApkVariant.ANDROID7
                else -> ApkVariant.UNIVERSAL
            }
        }

        fun selectBestAsset(
            assets: List<ReleaseAsset>,
            isUncompressed: Boolean,
            sdkInt: Int,
        ): ReleaseAsset? {
            val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) && it.downloadUrl.isNotBlank() }
            if (apkAssets.isEmpty()) return null

            val target = when {
                sdkInt < 29 -> ApkVariant.ANDROID7
                isUncompressed -> ApkVariant.UNCOMPRESSED
                else -> ApkVariant.UNIVERSAL
            }

            // Direct match for target variant, preferring "universal" naming if present
            val matches = apkAssets.filter { classifyAsset(it.name) == target }
            if (matches.isNotEmpty()) {
                return matches.maxByOrNull { asset ->
                    val lower = asset.name.lowercase()
                    when (target) {
                        ApkVariant.UNIVERSAL -> if (lower.contains("universal")) 2 else 1
                        ApkVariant.UNCOMPRESSED -> if (lower.contains("universal")) 2 else 1
                        ApkVariant.ANDROID7 -> if (lower.contains("android7")) 2 else 1
                    }
                }
            }

            // Fallback hierarchy if target variant was not present in this release:
            return when (target) {
                ApkVariant.UNCOMPRESSED -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNIVERSAL }
                        ?: apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.ANDROID7 }
                        ?: apkAssets.firstOrNull()
                }
                ApkVariant.UNIVERSAL -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNCOMPRESSED }
                        ?: apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.ANDROID7 }
                        ?: apkAssets.firstOrNull()
                }
                ApkVariant.ANDROID7 -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNIVERSAL }
                        ?: apkAssets.firstOrNull()
                }
            }
        }
    }
}
