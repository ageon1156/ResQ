
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.hilt)
}

configure<LibraryExtension> {
    namespace = "org.meshtastic.core.audio"
}

dependencies {
    implementation(libs.kermit)
}
