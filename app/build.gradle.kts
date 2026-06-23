plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.chatforia.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chatforia.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] =
            project.findProperty("ADMOB_APP_ID")
                ?.toString()
                ?: "ca-app-pub-3940256099942544~3347511713"


        buildConfigField(
            "String",
            "POSTHOG_API_KEY",
            "\"${project.findProperty("POSTHOG_API_KEY") ?: ""}\""
        )

        buildConfigField(
            "String",
            "POSTHOG_HOST",
            "\"${project.findProperty("POSTHOG_HOST") ?: "https://us.i.posthog.com"}\""
        )
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )

            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )
        }

        release {
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"${project.findProperty("ADMOB_REAL_BANNER_AD_UNIT_ID") ?: "ca-app-pub-3940256099942544/9214589741"}\""
            )

            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_AD_UNIT_ID",
                "\"${project.findProperty("ADMOB_REAL_INTERSTITIAL_AD_UNIT_ID") ?: "ca-app-pub-3940256099942544/1033173712"}\""
            )

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("com.google.android.gms:play-services-ads:25.4.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.posthog:posthog-android:3.47.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }

    implementation("com.goterl:lazysodium-android:5.1.0") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }

    implementation("com.twilio:voice-android:6.9.0")
    implementation("com.twilio:video-android:7.8.0")

    implementation("net.java.dev.jna:jna:4.5.1@aar")

    implementation("androidx.credentials:credentials:1.7.0-alpha02")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha02")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation(libs.junit)
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}