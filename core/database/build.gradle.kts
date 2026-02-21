
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.meshtastic.android.library)
    alias(libs.plugins.meshtastic.android.room)
    alias(libs.plugins.meshtastic.hilt)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

configure<LibraryExtension> {
    namespace = "org.meshtastic.core.database"
    sourceSets {
        
        named("androidTest") { assets.srcDirs(files("$projectDir/schemas")) }
    }
}

dependencies {
    implementation(projects.core.di)
    implementation(projects.core.model)
    implementation(projects.core.proto)
    implementation(projects.core.strings)

    implementation(libs.androidx.room.paging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

