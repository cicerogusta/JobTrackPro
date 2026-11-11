pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "JobTrackPro"
include(":app", ":core:common", ":core:model", ":core:database", ":core:network", ":core:data", ":core:designsystem",
    ":feature:home", ":feature:auth", ":feature:applications", ":feature:jobs", ":feature:settings")
project(":core:designsystem").projectDir = file("app/designsystem")
project(":feature:home").projectDir = file("app/home")
