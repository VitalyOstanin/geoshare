// Libraries the build tools bring with them, raised past their known
// vulnerabilities: the Android Gradle plugin (Bouncy Castle, jose4j, JDOM,
// HttpClient, Commons Lang) and the CycloneDX plugin (Jackson) pin older ones
// than have been fixed. None of them ends up in the application; they run on
// the machine that builds it. Each is a floor rather than a pin, so a plugin
// asking for a newer one still gets it.
buildscript {
    dependencies {
        constraints {
            add("classpath", "org.bouncycastle:bcprov-jdk18on:1.86")
            add("classpath", "org.bouncycastle:bcpkix-jdk18on:1.86")
            add("classpath", "org.bouncycastle:bcutil-jdk18on:1.86")
            add("classpath", "com.fasterxml.jackson.core:jackson-core:2.22.3")
            add("classpath", "com.fasterxml.jackson.core:jackson-databind:2.22.3")
            add("classpath", "org.bitbucket.b_c:jose4j:0.9.7")
            add("classpath", "org.jdom:jdom2:2.0.6.1")
            add("classpath", "org.apache.commons:commons-lang3:3.21.0")
            add("classpath", "org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.cyclonedx) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
}

// Read out here because inside the blocks below `libs` would be looked up on
// the project being configured, which has no version catalogue.
val ktlintVersion = libs.versions.ktlint.get()

// Applied to the root project as well as the application: the build scripts
// are Kotlin too.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintVersion)
    }

    // ktlint's own command line brings Logback 1.3, whose fixes are only in
    // 1.5; the two halves of Logback have to be the same version.
    configurations.matching { it.name.startsWith("ktlint") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "ch.qos.logback") {
                useVersion("1.5.38")
                because("Logback 1.3 has known vulnerabilities fixed in 1.5 only")
            }
        }
    }
}
