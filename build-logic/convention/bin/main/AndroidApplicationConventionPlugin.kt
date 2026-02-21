

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.meshtastic.buildlogic.configureKotlinAndroid

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            apply(plugin = "com.android.application")
            apply(plugin = "meshtastic.android.lint")
            apply(plugin = "meshtastic.detekt")
            apply(plugin = "meshtastic.spotless")
            apply(plugin = "meshtastic.analytics")
            apply(plugin = "meshtastic.kover")
            apply(plugin = "meshtastic.dokka")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                
                defaultConfig {
                    testInstrumentationRunner = "com.geeksville.mesh.TestRunner"
                    vectorDrawables.useSupportLibrary = true
                }

                testOptions.animationsDisabled = true

                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                    getByName("debug") {
                        isDebuggable = true
                        isPseudoLocalesEnabled = true
                        enableAndroidTestCoverage = true
                    }
                }

                buildFeatures {
                    buildConfig = true
                }
            }
        }
    }
}

