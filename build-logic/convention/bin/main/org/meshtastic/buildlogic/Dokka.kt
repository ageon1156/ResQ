

package org.meshtastic.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension
import java.net.URI

fun Project.configureDokka() {
    extensions.configure<DokkaExtension> {
        
        moduleName.set(project.path.removePrefix(":").replace(":", "-").ifEmpty { project.name })

        dokkaSourceSets.configureEach {
            perPackageOption {
                matchingRegex.set("hilt_aggregated_deps")
                suppress.set(true)
            }
            perPackageOption {
                matchingRegex.set("org.meshtastic.core.strings.*")
                suppress.set(true)
            }

            val isCoreSourceSet = name in listOf("main", "commonMain", "androidMain", "fdroid", "google")
            if (isCoreSourceSet) {
                suppress.set(false)
            }

            sourceLink {
                enableJdkDocumentationLink.set(true)
                enableKotlinStdLibDocumentationLink.set(true)
                reportUndocumented.set(true)

                localDirectory.set(project.projectDir)
                val relativePath = project.projectDir.relativeTo(rootProject.projectDir).path.replace("\\", "/")
                remoteUrl.set(URI("https://github.com/meshtastic/Meshtastic-Android/blob/main/$relativePath"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

fun Project.configureDokkaAggregation() {
    extensions.configure<DokkaExtension> {
        moduleName.set("Meshtastic App")
        dokkaPublications.configureEach {
            suppressInheritedMembers.set(true)
        }
    }

    subprojects.forEach { subproject ->
        subproject.pluginManager.withPlugin("org.jetbrains.dokka") {
            dependencies.add("dokka", subproject)
        }
    }
}

