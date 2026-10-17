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
        versionCode = 33
        // Phase 29: Production version. Semantic versioning starts at 1.0.0.
        // 1.0.1: disconnect-crash fix + always-visible Connect button.
        // 1.0.2: circular no-padding launcher icon.
        // 1.0.3: user's own circular logo (round) + OG square logo (square).
        // 1.0.4: user's circular file used EXACTLY (black bg kept, 72dp safe
        //        zone so no launcher mask can zoom/crop it).
        // 1.0.5: sync hardening — real error surfaced in the failure message,
        //        per-thread save isolation, stale "later phase" text fixed.
        // 1.0.6: dark-mode contrast fix (Connect screen); sync progress with
        //        real %; 8x concurrent fetch (~5min -> <2min); animated
        //        import flow with flying-mail progress for new accounts.
        // 1.0.7: ROOT-CAUSE fix for "100% but no mails" — FTS indexing is now
        //        best-effort (devices without FTS5 no longer abort every
        //        save); per-message isolation narrowed to the mapping layer
        //        so systemic save failures stay loud; progress label handles
        //        processed > estimate.
        // 1.0.8: adaptive icon background solid black (was transparent) —
        //        matches the logo file's own background; more robust across
        //        launchers. Icon resources otherwise byte-identical to 1.0.4.
        // 1.0.9: Gmail-fidelity UI overhaul — mail list rows mirror Gmail's
        //        exact row layout (avatar/sender+time/subject/snippet/star,
        //        88dp, no dividers, pull-to-refresh, animateItem); thread
        //        view uses Gmail-style message cards with "to me" expander;
        //        12 rainbow categories consolidated into 5 neutral groups.
        // 1.0.10: company logos in the Companies section — real logos from
        //        the user's ICONS-ASSETS repo in 40dp circles (8 bundled for
        //        offline: Adobe, Amazon, AWS, Apple, ClickUp, Duolingo,
        //        Devpost, Alibaba Cloud); on-demand loader fetches any of
        //        the repo's ~1000 icons with memory+disk cache; letter
        //        avatar fallback. Standing no-emoji rule: app-created emojis
        //        stripped from sample data, never added anywhere.
        // 1.0.16: Gmail-style infinite scroll (no Load more button,
        //        prefetch at halfway, footer spinner); jelly-bubble
        //        bottom-nav indicator + Appearance picker (shared
        //        squash-stretch pill); Analytics range chip renamed
        //        "All synchronized" -> "All time", chip row scrolls.
        // 1.0.17: bottom-bar geometry fix (nav-bar padding outside the
        //        fixed 80dp height — bar sits clear above the system
        //        nav buttons again); square tap ripple removed from
        //        the pill controls (nav tabs + Appearance picker).
        // 1.0.18: hoisted dynamic top bar (hamburger slides in, pill
        //        reshapes), directional tab slides, skeleton loading,
        //        scroll edge fade, dark logo backing, 300ms Mail-list
        //        defer; top-bar 16dp edge margin; one-shot drawer-open
        //        event (SharedFlow, no replay).
        // 1.0.19: Gmail-fidelity drawer hoisted above the Scaffold
        //        (covers top + bottom bars), 48dp rows, 14sp labels,
        //        selected pill; hamburger opens via DrawerState.
        // 1.0.20: drawer auto-open/freeze fix — drawer content is
        //        ALWAYS composed (a conditional content changed the
        //        sheet width and corrupted the drawer anchors, so it
        //        opened by itself on Mail entry and stuck the scrim
        //        on leaving Mail); drawer closes on leaving Mail.
        // 1.0.21: skeleton only on the true first load — the 300ms
        //        skeleton defer on every Mail tab entry is removed;
        //        once the mailbox is cached, tab switches show the
        //        cached list immediately.
        // 1.0.22: Instagram-style pull-to-refresh — one hoisted
        //        PullToRefreshBox over the whole mailbox with a
        //        custom pill indicator (filling arc + "Pull to
        //        refresh" / "Release to refresh" / "Refreshing…");
        //        the top-bar reload button is removed (the gesture
        //        is the reload). The top-bar account button now
        //        shows the account's DP in every mode — unified
        //        inbox shows the default account's DP instead of
        //        the confusing envelope icon.
        // 1.0.23: Gmail-style account switcher sheet — tapping the
        //        top-bar DP opens a bottom sheet on any tab
        //        (current selection + collapse arrow, the other
        //        accounts, "Add another account" + "Settings");
        //        the three-dots overflow menu is removed from all
        //        tabs (Gmail parity — the DP is the only trailing
        //        action, Settings/Privacy/Integrations/Accounts all
        //        live inside the Settings page).
        // 1.0.24: top-bar DP uniform 40dp on every tab; demo accounts
        //        never sync (refresh finishes immediately when nothing
        //        syncable); Settings panel REPLACES the bottom-sheet
        //        switcher (DP tap opens full-screen Settings sliding
        //        in from the right, expandable account section at top).
        // 1.0.25: Settings-panel jump fixed (Scaffold gone — full-screen
        //        NavHost + overlaid bars, never resized); WhatsApp
        //        shift (underlying page slides 1/12 left as the panel
        //        slides in, slides back on close); scroll-hide bars
        //        BUILT (scroll down hides top+bottom bars, scroll up
        //        shows them, list expands, fade reaches the edges);
        //        "Action required" chip removed from the Mail list
        //        (Actions tab covers it; list + fade lifted to the
        //        search box).
        versionName = "1.0.32"

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
    // OAuth2 via Chrome Custom Tab (official Google-supported flow).
    // Tokens secured with EncryptedSharedPreferences.
    // Gmail API accessed via direct HTTPS (no heavy Google client libs).
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)

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
