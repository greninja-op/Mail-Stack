pluginManagement {
    repositories {
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
    }
}
rootProject.name = "mailstack-shared"
