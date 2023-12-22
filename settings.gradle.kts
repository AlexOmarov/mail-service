@file:Suppress("UnstableApiUsage")

rootProject.name = "mail-service"

include("mail-service-app", "mail-service-api")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
/*        maven {
            url = uri(System.getenv("PRIVATE_REPO_URL"))
            name = "PrivateRepo"
            credentials(HttpHeaderCredentials::class) {
                name = "Token"
                value = System.getenv("PRIVATE_REPO_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }*/
    }
}
