// Standalone desktop build that compiles the app's pure-Compose UI (app/.../ui) together
// with :core and renders each screen to a PNG. It needs no Android SDK, so it runs anywhere.
// Pinned to Compose Multiplatform 1.5 because that release resolves from Maven Central alone;
// UI code must therefore stay within Compose APIs that exist in both.
plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.5.12"
    application
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

kotlin {
    sourceSets["main"].kotlin.srcDirs(
        "../../core/src/main/kotlin",
        "../../app/src/main/java/com/karin/kaminplay/ui",
        "src/main/kotlin",
    )
}

application { mainClass.set("PreviewKt") }
