
# Zscaler Android SDK

Zscaler Development Kit (Zscaler SDK), part of the Zscaler Zero Trust Exchange™ platform, combines a set of robust capabilities to protect the integrity of your intellectual property, secure network communications from your mobile application, and prevent breaches against your APIs and core backend services.


## Integration
1. Add the Github repository to the dependencyResolutionManagement repositories in settings.gradle.
```
dependencyResolutionManagement {
    repositories {
        maven {
            name = "ZscalerSDKAndroid"
            url = uri("https://maven.pkg.github.com/zscaler/zscaler-sdk-android")
        }
    }
}
```

2. Install Zscaler SDK in the dependencies section of build.gradle.
```
dependencies {
    implementation("com.zscaler.sdk:zscalersdk-android:latest.release")
}
```

