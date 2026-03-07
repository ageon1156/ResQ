

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.datadog.gradle.plugin.DdExtension
import com.datadog.gradle.plugin.InstrumentationMode
import com.datadog.gradle.plugin.SdkCheckLevel
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.meshtastic.buildlogic.libs
import org.meshtastic.buildlogic.plugin

class AnalyticsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            
            extensions.configure<ApplicationExtension> {
                productFlavors.all {
                    if (name == "google") {
                        apply(plugin = libs.plugin("google-services").get().pluginId)
                        apply(plugin = libs.plugin("firebase-crashlytics").get().pluginId)
                        apply(plugin = libs.plugin("datadog").get().pluginId)
                    }
                }
            }




            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    if (variant.flavorName == "google") {
                        extensions.findByType<DdExtension>()?.apply {
                            variants {
                                register(variant.name) {
                                    site = "US5"
                                    composeInstrumentation = InstrumentationMode.AUTO
                                }
                            }
                            checkProjectDependencies = SdkCheckLevel.NONE
                        }
                    }
                }
            }
        }
    }
}

