pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "ZdkAndroid"
            url = uri("https://maven.pkg.github.com/zscaler/zscaler-sdk-android")
        }
    }
}

rootProject.name = "ZDK Demo"
include(":app")
