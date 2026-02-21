

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.meshtastic.buildlogic.configureDokka
import org.meshtastic.buildlogic.library
import org.meshtastic.buildlogic.libs

class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.dokka")

            dependencies {
                add("dokkaPlugin", libs.library("dokka-android-documentation-plugin"))
            }

            configureDokka()
        }
    }
}

