

package org.meshtastic.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {

    commonExtension.apply {
        compileSdk = configProperties.getProperty("COMPILE_SDK").toInt()

        defaultConfig.apply {
            minSdk = configProperties.getProperty("MIN_SDK").toInt()
            if (commonExtension is ApplicationExtension) {
                commonExtension.defaultConfig.targetSdk = configProperties.getProperty("TARGET_SDK").toInt()
            }
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

internal fun Project.configureKotlinMultiplatform() {
    extensions.configure<KotlinMultiplatformExtension> {
        
        pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            extensions.findByType<KotlinMultiplatformAndroidLibraryTarget>()?.apply {
                compileSdk = configProperties.getProperty("COMPILE_SDK").toInt()
                minSdk = configProperties.getProperty("MIN_SDK").toInt()

                if (namespace == null) {
                    val pkg = this@configureKotlinMultiplatform.path.removePrefix(":").replace(":", ".")
                    namespace = "org.meshtastic.$pkg"
                }
            }
        }
    }

    configureKotlin<KotlinMultiplatformExtension>()
}

internal fun Project.configureKotlinJvm() {
    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() {
    extensions.configure<T> {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(false)
            freeCompilerArgs.addAll(
                
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xcontext-parameters",
                "-Xannotation-default-target=param-property"
            )
        }
    }
}

