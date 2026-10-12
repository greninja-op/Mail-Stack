import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Room schema export (Phase 2): keeps every DB version's schema JSON under
// app/schemas so migrations stay reviewable and testable.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Stable debug signing key (2026-10-11): the sandbox Gradle auto-generated
// debug keystore is NOT stable across build agents (different APKs came out
// with different certs), and the Google OAuth Android client binds the
// package name to ONE SHA-1. This keeps every Mailstack APK on the same
// certificate so the Cloud Console registration — and on-device APK updates,
// which require matching certs — keep working. Falls back to the default
// debug key on machines without this file. (Debug key only; the password is
// not a secret.)
val stableDebugKeyFile = file("/home/hatch/keys/mailstack-debug.keystore")

android {
    namespace = "com.greninjaop.mailorganizer"
    // compileSdk 35: verified against AAR metadata; the newest AndroidX
    // releases need SDK 36/37 which are not yet usable in this environment.
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.greninjaop.mailorganizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        // Phase 29: Production version. Semantic versioning starts at 1.0.0.
        // 1.0.1: disconnect-crash fix + always-visible Connect button.
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (stableDebugKeyFile.exists()) {
            create("stableDebug") {
                storeFile = stableDebugKeyFile
                storePassword = "mailstack-debug"
                keyAlias = "mailstack-debug"
                keyPassword = "mailstack-debug"
            }
        }
    }

    buildTypes {
        debug {
            // Pin every debug APK to the stable certificate when available.
            if (stableDebugKeyFile.exists()) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<Test> {
    maxHeapSize = "1g"
    testLogging {
        events("passed", "failed", "skipped")
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Coroutines — async foundation for sync, DB and UI state (Phase 0+).
    implementation(libs.kotlinx.coroutines.android)

    // Room — KMP-compatible SQLite persistence. The local database is the
    // rebuildable intelligence layer (Phase 2 expands the schema).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences — app settings and (later) account-scoped prefs.
    implementation(libs.androidx.datastore.preferences)

    // Phase 3: Google OAuth & Gmail Connection
    // OAuth2 via system browser (official Google-supported flow).
    // Tokens secured with EncryptedSharedPreferences.
    // Gmail API accessed via direct HTTPS (no heavy Google client libs).
    implementation(libs.androidx.security.crypto)

    // Compose (versions managed by the Compose BOM)
    val bom = libs.compose.bom
    implementation(platform(bom))
    androidTestImplementation(platform(bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // ---- Test foundation (all actually exercised in Phase 0) ----
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine) // Flow testing
    testImplementation(libs.robolectric) // JVM integration tests (Room)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core) // ApplicationProvider for Robolectric

    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

tasks.register("printTestClasspath") {
    doLast {
        val testTask = tasks.named<Test>("testDebugUnitTest").get()
        println("###TEST_CP###" + testTask.classpath.asPath)
    }
}
