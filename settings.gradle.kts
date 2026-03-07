

include(
    ":app",
    ":core:analytics",
    ":core:common",
    ":core:data",
    ":core:audio",
    ":core:database",
    ":core:datastore",
    ":core:di",
    ":core:model",
    ":core:navigation",
    ":core:network",
    ":core:prefs",
    ":core:proto",
    ":core:service",
    ":core:strings",
    ":core:ui",
    ":feature:intro",
    ":feature:map",
    ":feature:messaging",
    ":feature:node",
    ":feature:settings",
    ":feature:emergency",
    ":feature:sos",
    ":feature:voicemessage",
    ":mesh_service_example",
)
rootProject.name = "MeshtasticAndroid"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver") version "1.0.0"
    id("com.gradle.develocity") version("4.3.1")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.4.0"
}

develocity {
    buildScan {
        capture {
            fileFingerprints.set(true)
        }
        publishing.onlyIf { false }
    }
    buildCache {
        local {
            isEnabled = true
        }

    }
}

@Suppress("UnstableApiUsage")
toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
            }
        }
    }
}
