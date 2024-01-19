plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

project.layout.buildDirectory = File("../.build/app")
val exclusions = project.properties["test_exclusions"].toString().replace("/", ".")

detekt {
    config.setFrom(files("$rootDir/detekt-config.yml"))
    reportsDir = file("${project.layout.buildDirectory.get().asFile.path}/reports/detekt")
}

kover {
    useJacoco()
}

koverReport {
    filters {
        excludes {
            classes(exclusions.split(","))
        }
    }
    defaults {
        xml {
            onCheck = true
        }
        log {
            onCheck = true
        }
        html {
            onCheck = true
        }

        verify {
            rule {
                minBound(50)
            }
        }
    }
}

dependencies {
    implementation(project(":mail-service-api"))

    detektPlugins(libs.detekt.ktlint)

    implementation(libs.spring.boot.starter.mail)
    implementation(libs.bundles.shedlock)
    implementation(libs.bundles.springdoc)
    implementation(libs.bundles.reactive.service.base)
    implementation(libs.bundles.database.postgresql)
    implementation(libs.bundles.grpc.server)

    testImplementation(libs.bundles.reactive.service.test)
}
springBoot {
    buildInfo()
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType(org.springframework.boot.gradle.tasks.bundling.BootJar::class.java) {
    loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC
}
