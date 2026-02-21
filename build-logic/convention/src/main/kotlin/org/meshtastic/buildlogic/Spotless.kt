

package org.meshtastic.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project

internal fun Project.configureSpotless(extension: SpotlessExtension) {
    val ktlintVersion = libs.version("ktlint")
    extension.apply {
        ratchetFrom("origin/main")
        kotlin {
            target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
            ktlint(ktlintVersion)
                .setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.kt"))
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
            ktlint(ktlintVersion)
                .setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
            licenseHeaderFile(
                rootProject.file("config/spotless/copyright.kts"),
                "(^(?![\\/ ]\\*).*$)"
            )
        }
    }
}

