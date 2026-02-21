

package org.meshtastic.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named

internal fun Project.configureDetekt(extension: DetektExtension) = extension.apply {
    toolVersion = libs.version("detekt")
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false

    source.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/jvmMain/kotlin",
        ),
    )

    tasks.named<Detekt>("detekt") {
        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(true)
            sarif.required.set(true)
            md.required.set(true)
        }
        
        reports.xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
        reports.html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.html"))
        reports.txt.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.txt"))
        reports.sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
        reports.md.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.md"))
    }
    dependencies {
        "detektPlugins"(libs.library("detekt-formatting"))
        "detektPlugins"(libs.library("detekt-compose"))
    }
}

