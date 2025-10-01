import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.firebase.crashlytics.buildtools.gradle.tasks.GenerateSymbolFileTask
import java.io.File;

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("com.google.gms.google-services") version "4.4.2"
    id("com.google.firebase.crashlytics") version "3.0.2"
    id("com.github.triplet.play") version "3.9.1" // for publishing test app bundle to play
}

val isAndroidTest = gradle.startParameter.taskNames.any {
    it.contains("connected", ignoreCase = true) || // connectedDebugAndroidTest etc.
    it.contains("androidTest", ignoreCase = true)
}

val versionFile = project.file("../VERSION.txt")
val versionText = versionFile.readText().trim()
val versionRegex = Regex("^(\\d+\\.\\d+\\.\\d+)$")
val matchResult = versionRegex.find(versionText)
var baseVersion = matchResult?.groupValues?.get(1) ?: "2.1.0"
var versionCodeVal = 1
if(project.hasProperty("buildBaseVersion")) {
    baseVersion = project.findProperty("buildBaseVersion").toString()
    println("baseVersion $baseVersion")
}
var finalVersion = baseVersion
rootProject.version =  baseVersion

var buildQualifier = "LOCAL"
if(project.hasProperty("buildQualifier")) {
    buildQualifier = project.findProperty("buildQualifier").toString()
}
if(buildQualifier != "") {
    finalVersion = "$finalVersion-$buildQualifier"
}

// calculate version code from final version name
val parts = finalVersion.split("-")[0].split(".") // handles SNAPSHOT also
val versionCodeInt = parts[0].toInt() * 10000 + parts[1].toInt() * 100 + (parts.getOrNull(2)?.toInt() ?: 0) // handles 0...99 for each major,minor,patch/build
if (versionCodeInt > versionCodeVal) {
    versionCodeVal = versionCodeInt
}

fun getGitRoot(): File {
    val os = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-parse --show-toplevel".split(" ")
        standardOutput = os
    }
    return File(String(os.toByteArray()).trim())
}

android {
    namespace = "com.zscaler.sdk.demoapp"
    compileSdk = 35

    // Add test config files from common location
    sourceSets {
        println("🔍 Checking sourceSets. isAndroidTest = $isAndroidTest")
        if (isAndroidTest) {
            val mainSourceSet = sourceSets.getByName("main")
            val testAssets = getGitRoot()
                .resolve("config/Tests/ConfigResources")
                .normalize()
            println("📁 TestAssets directory path resolved to: $testAssets")
            if (testAssets.exists()) {
                println("✅ TestAssets directory exists, include files as app assets")
                mainSourceSet.assets.setSrcDirs(
                    mainSourceSet.assets.srcDirs + testAssets
                )
            } else {
                throw GradleException("❌ TestAssets directory not found at: $testAssets")
            }
        }
    }

    defaultConfig {
        applicationId = "com.zscaler.sdk.android.testapp"
        minSdk = 28
        targetSdk = 35
        versionCode = versionCodeVal
        versionName = finalVersion

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
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
            version = finalVersion
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.android.volley:volley:1.2.1")
    // zdk dependency
    implementation("com.zscaler.sdk:zscalersdk-android:latest.release") {
        // when we import this dep with --include-build, we need to set the 
        // build type attribute so that proguard is tested properly
        attributes {
            attribute(com.android.build.api.attributes.BuildTypeAttr.ATTRIBUTE, project.objects.named(
                com.android.build.api.attributes.BuildTypeAttr::class.java, "release"
            ))
        }
    }


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
    androidTestUtil("androidx.test:orchestrator:1.5.1")
    androidTestImplementation("com.google.code.gson:gson:2.10.1")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

}
