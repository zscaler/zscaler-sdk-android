

# Zscaler SDK for Mobile Apps - Android

Zscaler Development Kit (Zscaler SDK), part of the Zscaler Zero Trust Exchange™ platform, combines a set of robust capabilities to protect the integrity of your intellectual property, secure network communications from your mobile application, and prevent breaches against your APIs and core backend services.


## Integration
1. Make sure gpr.user and gpr.key are set to your github username and access token in global gradle.properties file at ~/.gradle/gradle.properties
2. Add the Github repository to the dependencyResolutionManagement repositories in settings.gradle.
```
dependencyResolutionManagement {
    repositories {
        maven {
            name = "ZscalerSDKAndroid"
            url = uri("https://maven.pkg.github.com/zscaler/zscaler-sdk-android")
            credentials {
                username = settings.extra.get("gpr.user") as String ?: System.getenv("GITHUB_USERNAME")
                password = settings.extra.get("gpr.key") as String ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

3. Install Zscaler SDK in the dependencies section of build.gradle.
```
dependencies {
    implementation("com.zscaler.sdk:zscalersdk-android:latest.release")
}
```
## Git Hub Access Token

Check the below link for generating the access token [[here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages)]
