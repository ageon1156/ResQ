

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.meshtastic.buildlogic.library
import org.meshtastic.buildlogic.libs

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                
                "ksp"(libs.library("kotlin-metadata-jvm"))

                "ksp"(libs.library("hilt.compiler"))
                "implementation"(libs.library("hilt-android"))
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.library("hilt-core"))
                }
            }

            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "dagger.hilt.android.plugin")
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
                dependencies {
                    "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                }
            }
        }
    }
}

