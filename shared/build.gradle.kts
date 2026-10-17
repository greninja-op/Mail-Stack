plugins {
    kotlin("multiplatform") version "2.0.21"
}

kotlin {
    // JVM target validates commonMain on Linux CI / this Cloud Agent.
    // iOS targets (iosArm64 / iosSimulatorArm64) require macOS + Xcode —
    // they are declared in comments for Phase 1 Mac setup, not enabled here
    // so Linux builds stay green.
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        val jvmMain by getting
        val jvmTest by getting
    }
}

// KMP exposes `jvmTest` / `allTests` (not root `test`).
