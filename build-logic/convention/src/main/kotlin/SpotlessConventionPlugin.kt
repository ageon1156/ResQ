

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.meshtastic.buildlogic.configureSpotless
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.plugin("spotless").get().pluginId)
            extensions.configure<SpotlessExtension> {
                configureSpotless(this)
            }
        }
    }
}

