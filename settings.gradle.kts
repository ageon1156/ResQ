/*
 * Licensed under GPL-3.0
 */

include(
    ":app",
    ":core:analytics",
    ":core:common",
    ":core:data",
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
    ":feature:messaging",
    ":feature:node",
    ":feature:settings",
    ":feature:emergency",
    ":feature:sos",
    ":mesh_service_example",
)
rootProject.name = "MeshtasticAndroid"

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
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
//        remote(HttpBuildCache::class.java) {
//            isAllowInsecureProtocol = true
//            // Replace with your selfhosted instance address
//            // see: https://docs.gradle.org/current/userguide/build_cache.html#sec:build_cache_setup_http_backend
//            url = uri("http://<your-build-cache-ip-here>:5071/cache/")
//
//            // Allow this machine to upload results to the cache
//            isPush = true
//
//        }
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
