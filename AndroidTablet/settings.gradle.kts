pluginManagement {
    repositories {
        // Prefer the phone-app offline mirror when present; fall back to remotes.
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.2"
        id("org.jetbrains.kotlin.android") version "2.3.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
    }
}
rootProject.name = "mailstack-android-tablet"
include(":app")
