

package org.meshtastic.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    (commonExtension as? ApplicationExtension)?.buildFeatures?.compose = true
    (commonExtension as? LibraryExtension)?.buildFeatures?.compose = true

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        "implementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.library("androidx-compose-ui-tooling"))
        "implementation"(libs.library("androidx-compose-runtime"))
        "runtimeOnly"(libs.library("androidx-compose-runtime-tracing"))
        "debugImplementation"(libs.library("androidx-compose-ui-tooling"))

        "androidTestImplementation"(libs.library("androidx-test-espresso-core"))
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
        fun Provider<*>.relativeToRootProject(dir: String) = map {
            isolated.rootProject.projectDirectory
                .dir("build")
                .dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)

        stabilityConfigurationFiles
            .add(isolated.rootProject.projectDirectory.file("compose_compiler_config.conf"))
    }
}

