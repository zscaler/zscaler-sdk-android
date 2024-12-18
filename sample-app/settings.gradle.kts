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
            credentials {
                username = settings.extra.get("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = settings.extra.get("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
	    }
    }
}

rootProject.name = "ZDK Demo"
include(":app")
