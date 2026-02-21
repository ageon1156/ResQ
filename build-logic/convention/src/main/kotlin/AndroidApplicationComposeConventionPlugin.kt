

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.meshtastic.buildlogic.configureAndroidCompose
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugin("compose-compiler").get().pluginId)
            extensions.configure<CommonExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}

