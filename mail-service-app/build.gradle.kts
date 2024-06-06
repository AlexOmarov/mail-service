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
    reports {
        verify {
            rule {
                minBound(50)
            }
        }
        total {
            verify {
                rule {
                    minBound(50)
                }
            }
            filters {
                excludes {
                    classes(exclusions.split(","))
                }
            }

            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
}

dependencies {
    implementation(project(":mail-service-api"))

    detektPlugins(libs.detekt.ktlint)

    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.web)
    implementation(libs.bundles.kafka)
    implementation(libs.bundles.database)

    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.rsocket)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.rsocket)

    implementation(libs.bundles.postgres)
    implementation(libs.bundles.redis)
    implementation(libs.bundles.micrometer)
    implementation(libs.bundles.shedlock)

    implementation(libs.rsocket.micrometer)
    implementation(libs.otel.zipkin)
    implementation(libs.logstash.logback.encoder)

    implementation(libs.hessian)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.launcher)

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
