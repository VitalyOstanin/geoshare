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
            add("classpath", "com.fasterxml.jackson.core:jackson-core:2.22.4")
            add("classpath", "com.fasterxml.jackson.core:jackson-databind:2.22.4")
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

// The lowest version of a build tool's library without its known
// vulnerabilities, the same as the build script's floors above, which are
// read before anything in this file is.
val toolFloors =
    mapOf(
        "org.bouncycastle:bcprov-jdk18on" to "1.86",
        "org.bouncycastle:bcpkix-jdk18on" to "1.86",
        "org.bouncycastle:bcutil-jdk18on" to "1.86",
        "com.fasterxml.jackson.core:jackson-core" to "2.22.4",
        "com.fasterxml.jackson.core:jackson-databind" to "2.22.4",
        "org.bitbucket.b_c:jose4j" to "0.9.7",
        "org.jdom:jdom2" to "2.0.6.1",
        "org.apache.commons:commons-lang3" to "3.21.0",
        "org.apache.httpcomponents:httpclient" to "4.5.14",
        "ch.qos.logback:logback-core" to "1.5.38",
        "ch.qos.logback:logback-classic" to "1.5.38",
    )

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

    // The same libraries reach the Android lint run and ktlint through
    // configurations of their own, which the build script's floors above do
    // not cover; ktlint's command line brings Logback 1.3, whose fixes are in
    // 1.5 only. A version below its floor is raised, never one above lowered.
    // Local copies, so that the rule holds no reference to this script.
    val floors = toolFloors
    // Dotted release numbers part by part; a version with anything but
    // numbers in it is left as it is.
    val olderThan = { version: String, than: String ->
        val have = version.split('.').map { it.toIntOrNull() }
        val want = than.split('.').map { it.toInt() }
        val parts = maxOf(have.size, want.size)
        val pairs = (0 until parts).map { (have.getOrElse(it) { 0 }) to want.getOrElse(it) { 0 } }
        val differ = pairs.firstOrNull { (x, y) -> x != y }
        null !in have && differ != null && differ.first!! < differ.second
    }
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val floor = floors["${requested.group}:${requested.name}"]
            val asked = requested.version
            if (floor != null && asked != null && olderThan(asked, floor)) {
                useVersion(floor)
                because("$asked has known vulnerabilities fixed by $floor")
            }
        }
    }
}
