
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

configure<LibraryExtension> { namespace = "org.meshtastic.core.navigation" }

dependencies { implementation(libs.kotlinx.serialization.core) }

