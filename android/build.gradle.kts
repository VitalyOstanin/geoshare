plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint)
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
}
