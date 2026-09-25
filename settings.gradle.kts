// Keep plugin-resolution dependencies on security-patched versions. The Android
// Gradle Plugin and its lint tooling are resolved before project build scripts,
// so this must be applied in settings as well as the root build script.
buildscript {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.apache.httpcomponents:httpclient:4.5.14",
            "org.jdom:jdom2:2.0.6.1",
            "org.bouncycastle:bcprov-jdk18on:1.85",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.apache.commons:commons-lang3:3.18.0",
            "org.bitbucket.b_c:jose4j:0.9.6",
        )
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Uptime"
include(":app")
