rootProject.name = "mail-service"

include("mail-service-app", "mail-service-api")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}