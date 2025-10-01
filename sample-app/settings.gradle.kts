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
        mavenLocal()
        google()
        mavenCentral()
        maven {
            name = "ZdkAndroid"
            url = uri("https://maven.pkg.github.com/zscaler/zscaler-sdk-android")
            credentials {
                username = System.getenv("GITHUB_USERNAME") ?: providers.gradleProperty("gpr.user").getOrNull()
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").getOrNull()
            }
	    }
    }
}

rootProject.name = "ZDK Demo"
include(":app")
