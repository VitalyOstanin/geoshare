import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component
import org.gradle.api.file.RegularFile
import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries.android)
    // The software bill of materials that goes onto the GitHub release next
    // to the APK; see the cyclonedxDirectBom block below.
    alias(libs.plugins.cyclonedx)
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
val setupPageUrl: String = providers.gradleProperty("setupPageUrl").get()

// Coverage is counted only when asked for, with -Pcoverage: the agent that
// counts it slows every test run, and an ordinary run has no use for the
// numbers. tools/coverage.sh is what passes it.
val measureCoverage: Boolean = providers.gradleProperty("coverage").isPresent

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
        buildConfigField("String", "SETUP_PAGE_URL", "\"$setupPageUrl\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        debug {
            enableUnitTestCoverage = measureCoverage
        }
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

    lint {
        // A string no screen shows is still one every translation has to keep
        // up, and nothing else notices it has gone unread.
        error += "UnusedResources"
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

// The list behind the licences screen. Who wrote a library and how to fund it
// are not what the screen is for, and only make the file larger.
//
// Gradle warns that debugCompileClasspath and debugRuntimeClasspath are
// resolved during configuration time: this plugin collects them there on
// purpose, as the price of working with the configuration cache
// (https://github.com/mikepenz/AboutLibraries/issues/1369).
aboutLibraries {
    // Nothing is fetched while building: the plugin reads licence texts off
    // the network with no timeout, and a connection that never answers held a
    // build for good. The texts come from config/licenses in this module.
    offlineMode = true

    collect {
        configPath = file("config")
    }

    export {
        excludeFields.addAll("developers", "funding")
    }

    // The build fails on a dependency under any licence not named here, so a
    // new one is read and agreed to before it ships. Each of these lets the
    // application be passed on under MIT with the notices the screen shows.
    // The Go licence (re2j) and the Play Integrity terms have no SPDX id: the
    // list keys them by a hash of their text, and the check reads the Go one
    // by its name, so both are named both ways.
    license {
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll(
            "Apache-2.0",
            "MIT",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ASDKL",
            "PCSDKToS",
            "2895aeae23f1ae6eaa282fe471e528c6",
            "Go License",
            "7546fd760989f8b6ad8de53f6c981adb",
            "Play Integrity API Terms of Service",
        )
    }
}

// A test stuck on a delay or a lock would hold the run until somebody stops it
// by hand. The whole suite takes a small part of this.
tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))
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
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(libs.aboutlibraries.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    // Overrides the Android stub org.json.* on the unit test classpath, which
    // answers every call with a default rather than parsing anything.
    testImplementation(libs.org.json)

    // The smoke tests on the emulator: the application starts, and the
    // settings and licences screens draw. The manifest artefact adds the empty
    // activity a Compose rule without an activity of its own draws into.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}

// The bill of materials of the release APK: only what the release build puts
// on its runtime classpath, so no test library, no debug-only tool and no
// build plugin is listed as something the user's phone carries. JSON only;
// the XML twin the plugin writes by default would be a second file to attach.
tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
    projectType = Component.Type.APPLICATION
    componentName = "io.github.vitalyostanin.geoshare"
    componentVersion = appVersionName
    includeConfigs = listOf("releaseRuntimeClasspath")
    xmlOutput.convention(null as RegularFile?)
}
