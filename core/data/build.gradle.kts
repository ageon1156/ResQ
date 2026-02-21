
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.hilt)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

configure<LibraryExtension> { namespace = "org.meshtastic.core.data" }

dependencies {
    implementation(projects.core.analytics)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.di)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.prefs)
    implementation(projects.core.proto)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.core.location.altitude)
    implementation(libs.androidx.paging.common)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

