
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

configure<LibraryExtension> { namespace = "org.meshtastic.feature.intro" }

dependencies {
    implementation(projects.core.strings)
    googleImplementation(projects.core.ui)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}

