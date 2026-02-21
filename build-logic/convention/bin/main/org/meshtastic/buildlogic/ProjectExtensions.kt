

package org.meshtastic.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.io.FileInputStream
import java.util.Properties

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

fun VersionCatalog.bundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).get()

fun VersionCatalog.plugin(alias: String): Provider<PluginDependency> =
    findPlugin(alias).get()

fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

val Project.configProperties: Properties
    get() {
        val properties = Properties()
        val propertiesFile = rootProject.file("config.properties")
        if (propertiesFile.exists()) {
            FileInputStream(propertiesFile).use { properties.load(it) }
        }
        return properties
    }

