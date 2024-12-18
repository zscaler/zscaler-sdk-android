import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.firebase.crashlytics.buildtools.gradle.tasks.GenerateSymbolFileTask

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("com.google.gms.google-services") version "4.4.2"
    id("com.google.firebase.crashlytics") version "3.0.2"
    id("com.github.triplet.play") version "3.9.1" // for publishing test app bundle to play
}

var versionNameVal = "x.x-dev"
var versionCodeVal = 1

if (project.hasProperty("buildVersionName")) {
    versionNameVal = project.ext.get("buildVersionName").toString()
    rootProject.version =  versionNameVal
    // calculate version code from version name
    val parts = versionNameVal.split("-")[0].split(".") // handles SNAPSHOT also
    val versionCodeInt = parts[0].toInt() * 10000 + parts[1].toInt() * 100 + (parts.getOrNull(2)?.toInt() ?: 0) // handles 0...99 for each major,minor,patch/build
    if (versionCodeInt > versionCodeVal) {
        versionCodeVal = versionCodeInt
    }
    println("versionNameVal " + versionNameVal)
    println("versionCodeVal " + versionCodeVal)
}

android {
    namespace = "com.zscaler.sdk.demoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zscaler.sdk.android.testapp"
        minSdk = 28
        targetSdk = 35
        versionCode = versionCodeVal
        versionName = versionNameVal

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += mutableSetOf<String>("armeabi-v7a" , "arm64-v8a") // Exclude x86 and x86_64
        }
    }

    signingConfigs {
        register("release") {
            if (System.getenv("KEYSTORE_FILENAME") != null && file(System.getenv("KEYSTORE_FILENAME")).exists()) {
                storeFile = file(System.getenv("KEYSTORE_FILENAME"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "key0" //System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_PASSWORD") // System.getenv("KEY_PASSWORD")
                // Enable Signing Versions
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Add this extension
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = file("../../../zdklibrary/app/build/intermediates/merged_native_libs/")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    publishing {
        singleVariant("release") {
            publishApk()
        }
    }

    if (System.getenv("API_PRIVATE_KEY_FILENAME") != null) {
        play {
            val keyfile: String =
            System.getenv("API_PRIVATE_KEY_FILENAME") ?: "Missing API_PRIVATE_KEY_FILENAME"
            serviceAccountCredentials.set(file(keyfile))
            track.set("internal")
            releaseStatus.set(ReleaseStatus.DRAFT)
            resolutionStrategy.set(ResolutionStrategy.AUTO)
        }
    }

    ndkVersion = "27.2.12479018"
    buildToolsVersion = "35.0.0"
}

afterEvaluate {
    // This provides a workaround for https://github.com/firebase/firebase-android-sdk/issues/5629
    tasks.withType<GenerateSymbolFileTask>().configureEach {
        mustRunAfter(tasks.withType<MergeNativeLibsTask>())
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = (System.getenv("ARTIFACT_GROUP_ID") ?: "com.zscaler.sdk") + ".zscalersdk-android"
            artifactId = "testapp"
            version = versionNameVal
            pom {
                packaging = "apk"
            }
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation ("androidx.compose.runtime:runtime-livedata:1.7.1")

    // Androidx Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-process:2.8.5")

    // WebView
    implementation("androidx.webkit:webkit:1.9.0")

    // retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // zdk dependency
    implementation("com.zscaler.sdk:zscalersdk-android:latest.release")

    // Firebase crashlytics
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
//    implementation("com.google.firebase:firebase-analytics") // enable it for analytics reporting if required
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("com.google.code.gson:gson:2.10.1")
}
