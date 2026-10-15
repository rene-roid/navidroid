plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "xyz.yuuki.navidroid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.netease.cloudmusic"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Media3's offline/cache APIs are all marked unstable; we use them deliberately.
        disable += "UnsafeOptInUsageError"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.palette)
    testImplementation(libs.junit)
    // Android's org.json is a stub in local unit tests; the real one runs the parsers.
    testImplementation(libs.json)
}
