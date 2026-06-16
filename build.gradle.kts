import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.koog:koog-agents:1.0.0")
    implementation("ai.koog:agents-features-event-handler:1.0.0")
    implementation("ai.koog:agents-features-trace:1.0.0")
    implementation("ai.koog:agents-features-opentelemetry:1.0.0")
    implementation("org.neo4j.driver:neo4j-java-driver:6.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.18")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:neo4j:1.21.4")
    testImplementation("ai.koog:agents-test:1.0.0")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.MainKt")
    // mainClass.set("Example")
}

tasks {
    val execute by registering(JavaExec::class) {
        group = "application"
        mainClass.set(
            if (project.hasProperty("mainClass")) {
                project.property("mainClass") as String
            } else {
                application.mainClass.get()
            },
        )
        classpath = sourceSets.main.get().runtimeClasspath
    }
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "build/generated-src/antlr/main")
        }
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
        reporter(ReporterType.HTML)
    }
    filter {
        exclude("**/style-violations.kt")
    }
}
