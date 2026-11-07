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
    }
}

rootProject.name = "LifeFlow"
include(":app")

if (!file("app/build.gradle.kts").exists() && file("workspace/app/build.gradle.kts").exists()) {
    project(":app").projectDir = file("workspace/app")
} else {
    project(":app").projectDir = file("app")
}
