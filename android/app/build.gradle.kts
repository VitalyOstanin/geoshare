plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
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

    testOptions {
        // The stub platform the JVM tests compile against throws on every
        // framework call, and the Firebase exception constructors reach
        // TextUtils to check their own message. Nothing here tests the
        // platform, so a default answer costs nothing and lets the tests use
        // the real exception types instead of stand-ins.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    // The application has no fragments; this states the floor the
    // activity-result API needs, over the old one Play Services carries.
    implementation(libs.androidx.fragment)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

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

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.osmdroid.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
