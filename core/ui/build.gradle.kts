
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.hilt)
}

configure<LibraryExtension> { namespace = "org.meshtastic.core.ui" }

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.prefs)
    implementation(projects.core.proto)
    implementation(projects.core.service)
    implementation(projects.core.strings)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.guava)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.kermit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)

    testImplementation(libs.junit)
}

