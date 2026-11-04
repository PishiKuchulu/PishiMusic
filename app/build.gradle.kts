import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/** ABIs for debug native builds, from -Ptuned.debugAbis. Null means all ABIs. */
fun debugAbiFilters(): Set<String>? {
    val value = (project.findProperty("tuned.debugAbis") as? String)?.trim() ?: "arm64-v8a"
    return if (value == "all") null else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

android {
    namespace = "com.tuned.app"
    compileSdk = 37

    val localProps = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    setProperty(parts[0].trim(), parts[1].trim())
                }
            }
        }
    }

    // Clean a secret value WITHOUT mangling it: only surrounding whitespace,
    // CR/LF and ONE pair of wrapping quotes are removed. The previous
    // implementation deleted every '"' and '\' anywhere in the value, which
    // silently corrupted legitimate passwords/tokens containing those
    // characters and surfaced later as inexplicable signing/auth failures.
    fun cleanSecretValue(raw: String): String {
        var v = raw.trim().replace("\r", "").replace("\n", "")
        if (v.length >= 2 && v.first() == '"' && v.last() == '"') {
            v = v.substring(1, v.length - 1)
        }
        return v
    }

    fun resolveSecret(vararg keys: String): String {
        for (key in keys) {
            val fromEnv = System.getenv(key)
            if (!fromEnv.isNullOrBlank()) return cleanSecretValue(fromEnv)
            val fromGradle = project.findProperty(key) as? String
            if (!fromGradle.isNullOrBlank()) return cleanSecretValue(fromGradle)
            val fromLocal = localProps.getProperty(key)
            if (!fromLocal.isNullOrBlank()) return cleanSecretValue(fromLocal)
            val dotEnv = rootProject.file(".env")
            if (dotEnv.isFile) {
                dotEnv.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("$key=")) {
                            val v = cleanSecretValue(trimmed.substringAfter("="))
                            if (v.isNotBlank()) return v
                        }
                    }
                }
            }
        }
        return ""
    }

    defaultConfig {
        // This app ID, a version code above the previous app's and its signing key let
        // Tuned install as that app's update and keep its data for LegacyImporter.
        // The code's package (namespace above) is com.tuned.app.
        applicationId = "com.tamed.music"
        // Pinned in source on purpose: a stray -PminSdk on some machine must
        // never silently change the shipped minimum (and with it the API
        // surface the code is tested against).
        minSdk = 29
        targetSdk = 35
        versionCode = 200
        versionName = "2026.10"

        val appleLyricsToken = resolveSecret("APPLE_LYRICS_TOKEN")
        val appleLyricsUrl = resolveSecret("APPLE_LYRICS_URL")
        buildConfigField("String", "APPLE_LYRICS_TOKEN", "\"$appleLyricsToken\"")
        buildConfigField("String", "APPLE_LYRICS_URL", "\"$appleLyricsUrl\"")

        // Services that need infrastructure this fork does not run yet.
        // Each feature turns itself off while its value is empty.
        buildConfigField("String", "DISCORD_APPLICATION_ID", "\"${resolveSecret("DISCORD_APPLICATION_ID")}\"")
        buildConfigField("String", "JAM_SERVER_URL", "\"${resolveSecret("JAM_SERVER_URL")}\"")
        buildConfigField("String", "UPDATE_GITHUB_REPO", "\"${resolveSecret("UPDATE_GITHUB_REPO").ifBlank { "PishiKuchulu/PishiMusic" }}\"")

        // Native secrets (addon client lock) live strictly in native .so via
        // SecretsBridge_generated.h (tools/generate_native_secrets.py).
        // No secret fields are exposed in DEX / BuildConfig.

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    // Android 15+ can boot with 16 KB memory pages; all native
                    // libraries must be built/aligned accordingly. Ignored
                    // harmlessly by NDK toolchains that predate the flag.
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DCMAKE_POLICY_VERSION_MINIMUM=3.5",
                )
                // NOTE: -Wl,-z,max-page-size=16384 is a LINKER flag and is set
                // correctly as a link option in CMakeLists.txt; it must not be
                // duplicated into cFlags/cppFlags (compile flags).
            }
        }
    }

    signingConfigs {
        create("release_config") {
            val base64Key = resolveSecret("SIGNING_KEY")
            val storeFilePath = resolveSecret("RELEASE_STORE_FILE")
            val storePasswordProp = resolveSecret("RELEASE_STORE_PASSWORD", "KEY_STORE_PASSWORD")
            val keyAliasProp = resolveSecret("RELEASE_KEY_ALIAS", "ALIAS").ifBlank { "release_key" }
            val keyPasswordProp = resolveSecret("RELEASE_KEY_PASSWORD", "KEY_PASSWORD").ifBlank { storePasswordProp }

            val keystoreFile: File? = when {
                base64Key.isNotBlank() -> {
                    try {
                        val decodedBytes = Base64.getDecoder().decode(base64Key.trim())
                        val tempKeystore = layout.buildDirectory.file("signing/release.keystore").get().asFile
                        tempKeystore.parentFile.mkdirs()
                        tempKeystore.writeBytes(decodedBytes)
                        tempKeystore
                    } catch (_: Exception) {
                        null
                    }
                }
                storeFilePath.isNotBlank() -> file(storeFilePath)
                else -> null
            }

            if (keystoreFile != null && keystoreFile.exists() && storePasswordProp.isNotBlank()) {
                storeFile = keystoreFile
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                project.extra.set("tunedReleaseSigningFallback", false)
            } else {
                // Never silently fall back to the debug key for a release
                // variant. A release APK signed with the runner's random debug
                // key installs once, then every future update fails with
                // INSTALL_FAILED_UPDATE_INCOMPATIBLE (signature conflict) and
                // forces the user to uninstall and lose their data. apksigner
                // verify only proves *a* signature is valid, not whose it is,
                // so we must refuse to produce the APK in the first place.
                //
                // Only fail when a release variant is actually being requested,
                // so debug builds and unit tests keep working with no signing
                // secrets present on a dev machine.
                val requestsRelease =
                    gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
                if (requestsRelease) {
                    throw GradleException(
                        "No release signing key found. Provide SIGNING_KEY (base64 " +
                            "keystore) or RELEASE_STORE_FILE + RELEASE_STORE_PASSWORD + " +
                            "RELEASE_KEY_ALIAS. Refusing to sign a release variant with " +
                            "the debug key."
                    )
                }
                initWith(getByName("debug"))
                // The task-name check above is only a fast path: aggregate
                // tasks (:app:assemble, :app:build, IDE builds, camel-case
                // abbreviations like `aR`) reach release packaging without
                // any requested task name containing "Release". Record the
                // fallback here; the task-graph guard after the android block
                // inspects the actual execution graph and refuses to package
                // any release variant signed with the debug key.
                project.extra.set("tunedReleaseSigningFallback", true)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release_config")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("rawRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            // Raw variant — no code/resource shrinking, no ProGuard/R8
            signingConfig = signingConfigs.getByName("release_config")
            // proguardFiles from initWith are ignored when minify is off
        }
        debug {
            isDebuggable = true
            // Debug builds compile native code for one ABI (arm64-v8a: current
            // phones and Apple Silicon emulators) instead of four, which cuts
            // native build time and APK size by roughly 4x.
            // Build every ABI with -Ptuned.debugAbis=all, or pick some with
            // -Ptuned.debugAbis=arm64-v8a,x86_64.
            debugAbiFilters()?.let { ndk { abiFilters += it } }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required by org.jellyfin.media3:media3-ffmpeg-decoder AAR metadata.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
        prefab = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        // Lint is a real gate: release builds are checked and lint ERRORS
        // fail the build (warnings remain non-fatal). Previously all three
        // switches were off, so no lint/a11y/i18n error could ever surface.
        checkReleaseBuilds = true
        abortOnError = true
        ignoreWarnings = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Release-signing guard at the task-graph level (see the signingConfigs block
// above). The graph contains the canonical packaging tasks no matter how the
// build was invoked — `:app:assemble`, `:app:bundle`, `:app:build`, IDE
// "Make Project", or an abbreviation like `aR` all end up scheduling
// packageRelease/assembleRelease/bundleRelease — so checking requested task
// names alone is not sufficient.
gradle.taskGraph.whenReady {
    val fallbackUsed = project.extra.properties["tunedReleaseSigningFallback"] == true
    if (fallbackUsed) {
        val producesReleaseArtifact = allTasks.any { task ->
            val n = task.name
            n.contains("Release") &&
                (n.startsWith("package") || n.startsWith("assemble") ||
                    n.startsWith("bundle") || n.startsWith("validateSigning"))
        }
        if (producesReleaseArtifact) {
            throw GradleException(
                "No release signing key found. Provide SIGNING_KEY (base64 " +
                    "keystore) or RELEASE_STORE_FILE + RELEASE_STORE_PASSWORD + " +
                    "RELEASE_KEY_ALIAS. Refusing to sign a release variant with " +
                    "the debug key."
            )
        }
    }
}

// Room exports its schema here (AppDatabase exportSchema = true) so schema
// changes are reviewable and migration tests can run against real files.
// The exported JSON is generated by the first build after this change.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("com.google.android.gms:play-services-cast-framework:22.3.1")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.palette)

    // The home-screen "Now Playing" widget is plain RemoteViews
    // (widget/WidgetViews.kt) — the Glance dependencies that used to sit
    // here had zero usages in the source tree and were removed.

    // Required even in a Compose-only app: Theme.Material3.DayNight.NoActionBar
    // (used as the AndroidManifest/splash theme parent in themes.xml) is an XML
    // style resource shipped by this artifact. androidx.compose.material3 is
    // Compose-only Kotlin and contributes no AAPT-resolvable style/ resources,
    // so without this dependency that parent can never be found by the linker.
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    // AppRouteNavigator's route whitelist is computed from
    // Screen::class.sealedSubclasses, which is a kotlin-reflect API: without
    // this dependency the singleton's initializer throws
    // KotlinReflectionNotSupportedError on first construction (launch crash,
    // on-device report 2026-11-04, FIX-LOG 102).
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.jsoup)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    // Lyrics sync uses the in-house reference implementation
    // (mirrors Metrolist/SimpMusic start-based active-index + per-frame
    // position, no external karaoke canvas). No gradle lyrics library.
    // Installs the baseline profiles bundled inside Compose (and other
    // androidx) AARs so hot UI paths are AOT-compiled on device instead of
    // running through JIT on first use — a large, zero-code smoothness win
    // for scrolling and animations in release builds.
    implementation(libs.androidx.profileinstaller)

    // Native in-app audio playback, background service, system media
    // controls, Bluetooth/headset controls and a MediaController-backed UI.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.hls)
    // Segmented provider-module path: DASH chunk source + CDM decryption.
    // Pinned to the same 1.2.1 line as exoplayer/hls to avoid binary mismatch.
    implementation(libs.androidx.media3.exoplayer.dash)
    // MediaBrowserServiceCompat/MediaSessionCompat bridge used by Android
    // Auto to browse the Tuned library and control the same player.
    implementation(libs.androidx.media)

    // GPLv3 Media3-matched FFmpeg software decoder (distribution must comply).
    // The renderer factory prefers FFmpeg for every codec it supports so all
    // devices decode through one deterministic, OEM-bug-free path; platform
    // decoders remain as automatic fallbacks.
    implementation(libs.jellyfin.media3.ffmpeg.decoder)

    // Core library desugaring required by the FFmpeg decoder AAR metadata.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Low-latency native output. Version 1.10 remains API-compatible with the
    // requested Oboe 1.8+ baseline and exposes its CMake target through Prefab.
    implementation("com.google.oboe:oboe:1.10.0")

    // Metadata/search remains local InnerTube/NewPipe functionality; playback
    // resolves through InnerTubeX first and retains NewPipe as a fallback.
    implementation(libs.newpipe.extractor)

    // InnerTubeX is invoked through the compatibility adapter because its
    // current release is built with a newer Kotlin metadata version than the
    // app. Runtime-only keeps the app compiler on its existing Kotlin line.
    runtimeOnly(libs.innertubex)
    runtimeOnly("io.ktor:ktor-client-cio:3.5.2")

    // Unit Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Bit-perfect USB exclusive output: audio_engine UsbAudioDriver.
    implementation(project(":audio:decent-usb-audio-driver"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

tasks.withType<Test> {
    maxHeapSize = "2048m"
}

// Generate native secrets header before CMake configures.
// CI provides ADDON_CLIENT_SECRET / RELEASE_CERT_SHA256 via env/secrets.
val generateNativeSecrets by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    val py = org.gradle.internal.os.OperatingSystem.current().let {
        if (it.isWindows) "python" else "python3"
    }
    commandLine(py, "tools/generate_native_secrets.py")
    // Never fail public builds when secrets absent; script emits empty header.
    isIgnoreExitValue = true
}
tasks.matching { it.name.startsWith("preBuild") || it.name.startsWith("configureCMake") }.configureEach {
    dependsOn(generateNativeSecrets)
}

