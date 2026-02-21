

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.meshtastic.buildlogic.configureFlavors
import org.meshtastic.buildlogic.configureKotlinAndroid
import org.meshtastic.buildlogic.disableUnnecessaryAndroidTests

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "meshtastic.android.lint")
            apply(plugin = "meshtastic.detekt")
            apply(plugin = "meshtastic.spotless")
            apply(plugin = "meshtastic.dokka")
            apply(plugin = "meshtastic.kover")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                configureFlavors(this)

                buildTypes {
                    getByName("debug") {
                        enableAndroidTestCoverage = true
                    }
                }
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnnecessaryAndroidTests(target)
            }
        }
    }
}

