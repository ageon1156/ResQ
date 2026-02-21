

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.meshtastic.buildlogic.library
import org.meshtastic.buildlogic.libs

class KotlinXSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            val serializationLib = libs.library("kotlinx-serialization-core")

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(serializationLib)
                    }
                }
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                dependencies {
                    "implementation"(serializationLib)
                }
            }
            
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(serializationLib)
                }
            }
        }
    }
}

