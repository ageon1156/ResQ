

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.meshtastic.buildlogic.configureDokkaAggregation
import org.meshtastic.buildlogic.configureGraphTasks
import org.meshtastic.buildlogic.configureKover
import org.meshtastic.buildlogic.configureKoverAggregation

class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target.path == ":")
        with(target) {
            apply(plugin = "org.jetbrains.dokka")
            configureDokkaAggregation()

            apply(plugin = "org.jetbrains.kotlinx.kover")
            configureKover()
            configureKoverAggregation()

            subprojects { configureGraphTasks() }
        }
    }
}

