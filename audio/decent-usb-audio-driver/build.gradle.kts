plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.decent.usbaudio"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        externalNativeBuild { cmake { cppFlags("-fno-fast-math") } }
    }

    buildTypes {
        getByName("debug") {
            // Same single-ABI debug default as :app (see tuned.debugAbis there).
            val abis = (project.findProperty("tuned.debugAbis") as? String)?.trim() ?: "arm64-v8a"
            if (abis != "all") ndk { abiFilters += abis.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
        }
        getByName("release")
        create("rawRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake { path("src/main/jni/CMakeLists.txt") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { jvmToolchain(17) }
}

dependencies {
    implementation(libs.androidx.core.ktx.audio)
}
