

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            when {
                pluginManager.hasPlugin("com.android.application") ->
                    configure<ApplicationExtension> { lint { configure(project) } }

                pluginManager.hasPlugin("com.android.library") ->
                    configure<LibraryExtension> { lint { configure(project) } }

                pluginManager.hasPlugin("com.android.kotlin.multiplatform.library") -> {
                    extensions.findByType<KotlinMultiplatformAndroidLibraryTarget>()?.apply {
                        @Suppress("UnstableApiUsage")
                        lint { configure(project) }
                    }
                }

                else -> {
                    apply(plugin = "com.android.lint")
                    configure<Lint> { configure(project) }
                }
            }
        }
    }
}

private fun Lint.configure(project: Project) {
    xmlReport = true
    sarifReport = true
    checkDependencies = true
    abortOnError = false
    disable += "GradleDependency"
}

