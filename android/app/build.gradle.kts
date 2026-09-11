plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The versions the build image installs, read from the one file that states
// them. Written out here as well, they would drift: a compileSdk the image has
// no platform for fails inside the container, and a build-tools version it
// does not ship is downloaded again on every run.
val toolVersions: Map<String, String> =
    rootProject
        .file("../tools/versions.env")
        .readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim() }

val appVersionName: String = providers.gradleProperty("appVersionName").get()
val appVersionCode: Int = providers.gradleProperty("appVersionCode").get().toInt()

android {
    namespace = "io.github.vitalyostanin.geoshare"
    compileSdk = toolVersions.getValue("ANDROID_COMPILE_SDK").toInt()
    buildToolsVersion = toolVersions.getValue("ANDROID_BUILD_TOOLS")

    defaultConfig {
        applicationId = "io.github.vitalyostanin.geoshare"
        // Both phones run Android 14; 29 leaves room for an older spare
        // without reaching back before scoped storage.
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        // Off by default since AGP 9; the application is Compose-only.
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // The Firebase libraries compile without google-services.json; the plugin
    // that consumes that file is added together with the file itself, once the
    // Firebase project exists.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
