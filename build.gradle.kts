import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin) // Sonarqube may break if this plugin will be in subprojects with version
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.sonarqube)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

sonar {
    val exclusions = project.properties["test_exclusions"].toString()
    val appBuildDirectory = project(":mail-service-app").layout.buildDirectory.get().asFile
    val apiBuildDirectory = project(":mail-service-api").layout.buildDirectory.get().asFile
    val reportPaths = "$appBuildDirectory/reports/detekt/detekt.xml, $apiBuildDirectory/reports/detekt/detekt.xml"

    properties {
        property("sonar.kotlin.detekt.reportPaths", reportPaths)
        property("sonar.qualitygate.wait", "true")
        property("sonar.core.codeCoveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "$appBuildDirectory/reports/kover/report.xml")
        property("sonar.cpd.exclusions", exclusions)
        property("sonar.jacoco.excludes", exclusions)
        property("sonar.coverage.exclusions", exclusions)
        property("sonar.junit.reportPaths", "$appBuildDirectory/test-results/test/")
    }
}
