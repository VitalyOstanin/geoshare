import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The versions the build image installs, read from the one file that states
// them. Written out here as well, they would drift: a compileSdk the image has
// no platform for fails inside the container, and a build-tools version it
// does not ship is downloaded again on every run.
// The file is read by a shell everywhere else, so what is read here has to
// survive the same things a shell would accept: a comment after a value, the
// word `export` in front of a name, quotes around a value. Split by hand, each
// of those became part of the number and the build failed a step later, where
// the cause was no longer in sight.
val toolVersions: Map<String, String> =
    Properties()
        .apply { rootProject.file("../tools/versions.env").reader().use { load(it) } }
        .map { (name, value) ->
            name.toString().removePrefix("export ").trim() to
                value.toString().substringBefore('#').trim().trim('"', '\'')
        }
        .toMap()

val appVersionName: String = providers.gradleProperty("appVersionName").get()
val appVersionCode: Int = providers.gradleProperty("appVersionCode").get().toInt()

android {
    namespace = "io.github.vitalyostanin.geoshare"
    compileSdk = toolVersions.getValue("ANDROID_COMPILE_SDK").toInt()
    buildToolsVersion = toolVersions.getValue("ANDROID_BUILD_TOOLS")

    defaultConfig {
        applicationId = "io.github.vitalyostanin.geoshare"
        // Android 8, for the oldest phone the invitation is tried on. Below
        // Android 10 the background location and the movement permissions do
        // not exist, and the code treats them as held there.
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            // The map draws in native code, and the library ships that code
            // for four architectures at about twelve megabytes each. Both
            // phones and the emulator are 64-bit, and nothing 32-bit runs the
            // Android this application asks for, so half of them are weight
            // without a reader.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
        // Only for DEBUG, which is what keeps the stand-in services out of a
        // published build.
        buildConfig = true
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
    implementation(libs.androidx.appcompat)
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

    // Which project a build talks to is decided at run time, off the settings
    // FamilyProject reads from the phone: nothing here reaches for
    // google-services.json, so one build serves every family.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation(libs.play.services.location)

    implementation(libs.maplibre.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    // Overrides the Android stub org.json.* on the unit test classpath, which
    // answers every call with a default rather than parsing anything.
    testImplementation(libs.org.json)
}
