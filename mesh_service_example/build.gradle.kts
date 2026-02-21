

import com.android.build.api.dsl.ApplicationExtension
import org.meshtastic.buildlogic.FlavorDimension
import org.meshtastic.buildlogic.MeshtasticFlavor

plugins {
    alias(libs.plugins.meshtastic.android.application)
    alias(libs.plugins.meshtastic.android.application.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

configure<ApplicationExtension> {
    namespace = "com.meshtastic.android.meshserviceexample"
    defaultConfig {
        
        missingDimensionStrategy(FlavorDimension.marketplace.name, MeshtasticFlavor.google.name)
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.proto)
    implementation(projects.core.service)
    implementation(projects.core.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

