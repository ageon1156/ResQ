

plugins {
    alias(libs.plugins.meshtastic.kmp.library)
    id("meshtastic.kmp.library.compose")
}

kotlin {
    @Suppress("UnstableApiUsage")
    androidLibrary { androidResources.enable = true }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.meshtastic.core.strings"
}

