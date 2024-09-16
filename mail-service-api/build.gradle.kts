plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    `maven-publish`
}

dependencies {
    detektPlugins(libs.detekt.ktlint)

    api(libs.springdoc.api)
    api(libs.kotlin.serialization.cbor)
    api(libs.spring.boot.starter.validation)
}

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            url = uri(System.getenv("PRIVATE_REPO_URL") ?: "")
            name = "PrivateRepo"
            credentials(HttpHeaderCredentials::class) {
                name = "Token"
                value = System.getenv("PRIVATE_REPO_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/detekt-config.yml"))
}

@Suppress("UnstableApiUsage") // getSupportedKotlinVersion is unstable for some reason
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}
