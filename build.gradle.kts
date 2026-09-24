// Top-level build file where you can add configuration options common to all subprojects/modules.

// Keep build-tool transitive dependencies on security-patched versions. These
// libraries are used by AGP/Lint during the build and are not packaged into the
// application runtime. The overrides are patch-level updates and preserve the
// APIs expected by AGP 9.4.1.
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

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
}
