
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.library.compose)
    alias(libs.plugins.meshtastic.hilt)
    alias(libs.plugins.secrets)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(projects.core.prefs)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.kermit)

    googleApi(libs.dd.sdk.android.compose)
    googleApi(libs.dd.sdk.android.logs)
    googleApi(libs.dd.sdk.android.rum)
    googleApi(libs.dd.sdk.android.session.replay)
    googleApi(libs.dd.sdk.android.session.replay.compose)
    googleApi(libs.dd.sdk.android.timber)
    googleApi(libs.dd.sdk.android.trace)
    googleApi(libs.dd.sdk.android.trace.otel)
    googleApi(platform(libs.firebase.bom))
    googleApi(libs.firebase.analytics)
    googleApi(libs.firebase.crashlytics)
}

configure<LibraryExtension> {
    buildFeatures { buildConfig = true }
    namespace = "org.meshtastic.core.analytics"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
    propertiesFileName = "secrets.properties"
}

