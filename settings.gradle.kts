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
rootProject.name = "Varta"
include(":app")
// Нешкідлива «приманка» для перевірки в емуляторі: має назву пакета відомої стежилки й просить ті самі права,
// але нічого не робить. У реліз не потрапляє.
include(":testspy")
