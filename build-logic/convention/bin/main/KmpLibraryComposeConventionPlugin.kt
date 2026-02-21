

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class KmpLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugin("compose-compiler").get().pluginId)
            apply(plugin = libs.plugin("compose-multiplatform").get().pluginId)

            val compose = extensions.getByType(ComposeExtension::class.java)
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(compose.dependencies.runtime)
                    
                    api(compose.dependencies.components.resources)
                }
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
    }
}

