

import androidx.room.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.meshtastic.buildlogic.library
import org.meshtastic.buildlogic.libs

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<KspExtension> {
                arg("room.generateKotlin", "true")
            }

            extensions.configure<RoomExtension> {

                schemaDirectory("$projectDir/schemas")
            }

            val roomRuntime = libs.library("androidx.room.runtime")
            val roomCompiler = libs.library("androidx.room.compiler")
            val roomTesting = libs.library("androidx-room-testing")

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(roomRuntime)
                    }
                }
                dependencies {
                    "kspCommonMainMetadata"(roomCompiler)
                    "kspAndroid"(roomCompiler)
                }
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                dependencies {
                    "implementation"(roomRuntime)
                    "ksp"(roomCompiler)
                    "androidTestImplementation"(roomTesting)
                }
            }
        }
    }
}

