import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/** A property from gradle.properties, ~/.gradle/gradle.properties, -P or OPENDIAL_* env vars. */
fun setting(name: String): String =
    providers.gradleProperty("opendial.$name")
        .orElse(providers.environmentVariable("OPENDIAL_" + name.replace('.', '_').uppercase()))
        .getOrElse("")
        .trim()

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// Google's sample app ID unless a real one is configured. The UMP SDK and the ads SDK both read it
// from the manifest.
val admobAppId = setting("admob.appId").ifEmpty { "ca-app-pub-3940256099942544~3347511713" }

android {
    namespace = "com.example.opendial"
    // Compose 1.13 needs the Android 17 QPR SDK (37.1 or later) to compile against.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.example.opendial"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId

        buildConfigField("String", "ADS_NETWORK", quoted(setting("ads.network").ifEmpty { "admob" }))
        buildConfigField("String", "ADMOB_APP_ID", quoted(admobAppId))
        buildConfigField("String", "BANNER_UNIT", quoted(setting("ads.bannerUnit")))
        buildConfigField("String", "INTERSTITIAL_UNIT", quoted(setting("ads.interstitialUnit")))
        buildConfigField("String", "REWARDED_UNIT", quoted(setting("ads.rewardedUnit")))
        buildConfigField("String", "APPSFLYER_DEV_KEY", quoted(setting("appsflyer.devKey")))
        buildConfigField("String", "META_APP_ID", quoted(setting("meta.appId")))
        buildConfigField("String", "META_CLIENT_TOKEN", quoted(setting("meta.clientToken")))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Roborazzi writes the README screenshots into docs/screenshots when recording.
                test.systemProperty("roborazzi.test.record", "true")
                test.systemProperty("opendial.screenshots", rootDir.resolve("docs/screenshots").absolutePath)
                test.testLogging {
                    events("passed", "failed")
                    exceptionFormat = TestExceptionFormat.FULL
                }
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        // targetSdk is held one below compileSdk on purpose until Android 17 behavior changes are reviewed.
        disable += "OldTargetApi"
        // Versions are pinned and checked by hand and by Dependabot; lint's own feed lags behind.
        disable += listOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// The GMA Next-Gen SDK replaces play-services-ads. Nothing may pull the legacy SDK back in.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.gma.nextgen)
    implementation(libs.gma.applovin.adapter)
    implementation(libs.appsflyer)
    implementation(libs.install.referrer)
    implementation(libs.facebook.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
