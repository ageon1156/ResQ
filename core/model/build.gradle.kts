
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

configure<LibraryExtension> {
    buildFeatures {
        buildConfig = true
        aidl = true
    }
    namespace = "org.meshtastic.core.model"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.proto)
    implementation(projects.core.strings)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)
    implementation(libs.zxing.android.embedded) { isTransitive = false }
    implementation(libs.zxing.core)

    testImplementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

