plugins {
    alias(libs.plugins.kotlin) // Sonarqube may break if this plugin will be in subprojects with version
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.sonarqube)
}

allprojects {
    group = "ru.somarov"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

var exclusions = project.properties["test_exclusions"].toString()

sonar {
    properties {
        property("sonar.qualitygate.wait", "true")
        property("sonar.core.codeCoveragePlugin", "jacoco")
        property(
            "sonar.kotlin.detekt.reportPaths",
            "${project(":mail-service-app").layout.buildDirectory.get().asFile}/reports/detekt/detekt.xml, " +
                "${project(":mail-service-api").layout.buildDirectory.get().asFile}/reports/detekt/detekt.xml"
        )
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project(":mail-service-app").layout.buildDirectory.get().asFile}/reports/kover/report.xml"
        )
        property("sonar.cpd.exclusions", exclusions)
        property("sonar.jacoco.excludes", exclusions)
        property("sonar.coverage.exclusions", exclusions)
    }
}
