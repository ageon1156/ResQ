
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.hilt)
}

configure<LibraryExtension> {
    namespace = "org.meshtastic.feature.sos"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.proto)
    implementation(projects.core.navigation)
    implementation(projects.core.service)
    implementation(projects.core.strings)
    implementation(projects.core.ui)

    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kermit)
}

