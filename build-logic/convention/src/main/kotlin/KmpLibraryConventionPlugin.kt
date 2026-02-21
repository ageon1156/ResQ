

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.meshtastic.buildlogic.configureKotlinMultiplatform
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugin("kotlin-multiplatform").get().pluginId)
            apply(plugin = libs.plugin("android-kotlin-multiplatform-library").get().pluginId)
            apply(plugin = "meshtastic.android.lint")
            apply(plugin = "meshtastic.detekt")
            apply(plugin = "meshtastic.spotless")
            apply(plugin = "meshtastic.dokka")
            apply(plugin = "meshtastic.kover")

            configureKotlinMultiplatform()
        }
    }
}

