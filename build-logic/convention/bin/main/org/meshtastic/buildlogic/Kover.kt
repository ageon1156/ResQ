

package org.meshtastic.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureKover() {
    extensions.configure<KoverProjectExtension> {
        reports {
            total {
                xml {
                    onCheck.set(true)
                }
                html {
                    onCheck.set(true)
                }
            }
            filters {
                excludes {
                    
                    classes("*_Impl")
                    classes("*Binding")
                    classes("*Factory")
                    classes("*.BuildConfig")
                    classes("*.R")
                    classes("*.R$*")

                    annotatedBy("*Preview")

                    annotatedBy(
                        "*.HiltAndroidApp",
                        "*.AndroidEntryPoint",
                        "*.Module",
                        "*.Provides",
                        "*.Binds",
                        "*.Composable",
                    )

                    packages("hilt_aggregated_deps")
                    packages("org.meshtastic.core.strings")
                }
            }
        }
    }
}

fun Project.configureKoverAggregation() {
    subprojects.forEach { subproject ->
        subproject.plugins.withId("org.jetbrains.kotlinx.kover") {
            dependencies.add("kover", subproject)
        }
    }
}

