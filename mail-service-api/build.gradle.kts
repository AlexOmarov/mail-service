import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.grpc.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spring.boot) apply false
    `maven-publish`
}
project.layout.buildDirectory = File("../.build/api")

detekt {
    config.setFrom(files("$rootDir/detekt-config.yml"))
    reportsDir = file("${project.layout.buildDirectory.get().asFile.path}/reports/detekt")
}

dependencies {
    detektPlugins(libs.detekt.ktlint)

    api(libs.bundles.springdoc)
    api(libs.bundles.grpc.client)

    implementation(libs.bundles.reactive.service.base)

    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}

sourceSets {
    val main by getting { }
    main.java.srcDirs("${project.layout.buildDirectory.get()}/generated/source/proto/main/kotlin")
    main.java.srcDirs("${project.layout.buildDirectory.get()}/generated/source/proto/main/grpckt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "20"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.22.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.52.1"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            url = uri(System.getenv("PRIVATE_REPO_URL"))
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
