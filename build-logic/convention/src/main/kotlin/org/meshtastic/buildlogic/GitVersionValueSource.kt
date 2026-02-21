

package org.meshtastic.buildlogic

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitVersionValueSource : ValueSource<String, GitVersionValueSource.Params> {
    interface Params : ValueSourceParameters
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        return try {
            execOperations.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                standardOutput = output
            }
            output.toString().trim()
        } catch (e: Exception) {
            throw RuntimeException("Failed to determine git commit count for versionCode. Ensure you have a full git history (not a shallow clone) and .git is present.\nOriginal error: ${e.message}", e)
        }
    }
}

