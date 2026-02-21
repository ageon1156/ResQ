
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

configure<LibraryExtension> {
    buildFeatures { buildConfig = true }
    namespace = "org.meshtastic.core.network"
}

dependencies {
    implementation(projects.core.di)
    implementation(projects.core.model)

    implementation(libs.coil.network.core)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.okhttp3.logging.interceptor)

    googleImplementation(libs.dd.sdk.android.okhttp)
}

