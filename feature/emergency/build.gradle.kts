
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.hilt)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

configure<LibraryExtension> {
    namespace = "org.meshtastic.feature.emergency"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.di)
    implementation(projects.core.navigation)
    implementation(projects.core.strings)
    implementation(projects.core.ui)

    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.common)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

