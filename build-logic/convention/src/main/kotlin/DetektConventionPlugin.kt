

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.meshtastic.buildlogic.configureDetekt
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.plugin("detekt").get().pluginId)
            extensions.configure<DetektExtension> {
                configureDetekt(this)
            }
        }
    }
}

