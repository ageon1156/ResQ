
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.hilt)
}

configure<LibraryExtension> { namespace = "org.meshtastic.feature.settings" }

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(projects.core.prefs)
    implementation(projects.core.proto)
    implementation(projects.core.service)
    implementation(projects.core.strings)
    implementation(projects.core.ui)

    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kermit)
    implementation(libs.zxing.android.embedded)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

