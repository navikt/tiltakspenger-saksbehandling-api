import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmVersion = JvmTarget.JVM_21
val kotlinxCoroutinesVersion = "1.10.2"
val kotestVersion = "5.9.1"
val felleslibVersion = "0.0.485"
val mockkVersion = "1.14.2"
val ktorVersion = "3.1.3"
val testContainersVersion = "1.21.1"
val poaoTilgangVersjon = "2025.06.06_07.18-71cefb1c2699"
val iverksettVersjon = "1.0_20241213145703_7ff5f9c"
val confluentVersion = "8.0.0"
val avroVersion = "1.12.0"
val prometeusVersion = "1.15.1"

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    implementation("com.github.navikt.tiltakspenger-libs:soknad-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:arenatiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:datadeling-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-ktor:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:kafka:$felleslibVersion")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("com.aallam.ulid:ulid-kotlin:1.3.0")

    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    implementation("com.natpryce:konfig:1.6.10.0")

    // Http
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")

    // Auth
    api("com.auth0:java-jwt:4.5.0")
    api("com.auth0:jwks-rsa:0.22.1")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:11.9.1")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.postgresql:postgresql:42.7.6")
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Helved/Utsjekk/Utbetaling
    implementation("no.nav.utsjekk.kontrakter:iverksett:$iverksettVersjon")

    //POAO tilgang
    implementation("no.nav.poao-tilgang:client:$poaoTilgangVersjon")

    // Avro
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")

    // DIV
    // Arrow
    implementation("io.arrow-kt:arrow-core:2.1.2")

    // Caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.1")

    testImplementation(platform("org.junit:junit-bom:5.13.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
    // need quarkus-junit-4-mock because of https://github.com/testcontainers/testcontainers-java/issues/970
    testImplementation("io.quarkus:quarkus-junit4-mock:3.23.2")
    testImplementation("io.github.serpro69:kotlin-faker:1.16.0")
    testApi("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:auth-test-core:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
}
plugins {
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
    kotlin("jvm") version "2.1.21"
    id("com.diffplug.spotless") version "7.0.4"
    application
}
application {
    mainClass.set("no.nav.tiltakspenger.saksbehandling.AppKt")
}
repositories {
    mavenCentral()
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
    }
}
tasks {
    kotlin {
        compilerOptions {
            jvmTarget.set(jvmVersion)
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }

    test {
        // JUnit 5 support
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // https://github.com/mockito/mockito/issues/3037#issuecomment-1588199599
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            // We only want to log failed and skipped tests when running Gradle.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
configurations.all {
    // exclude JUnit 4
    exclude(group = "junit", module = "junit")
}
tasks {
    register<Copy>("gitHooks") {
        from(file(".scripts/pre-commit"))
        into(file(".git/hooks"))
    }

    build {
        dependsOn("gitHooks")
    }

    register("checkFlywayMigrationNames") {
        doLast {
            val files = project.file("src/main/resources/db/migration").walk()
                .filter { it.isFile && it.extension == "sql" }
                .toList()

            val invalidFiles = files
                .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.sql")) }
                .map { it.name }

            if (invalidFiles.isNotEmpty()) {
                throw GradleException("Invalid migration filenames:\n${invalidFiles.joinToString("\n")}")
            }

            val duplicateVersions = files
                .mapNotNull { it.name.split("__").firstOrNull()?.removePrefix("V")?.toIntOrNull() }
                .groupBy { it }
                .filter { it.value.size > 1 }
                .keys

            if (duplicateVersions.isNotEmpty()) {
                throw GradleException("Duplicate version numbers found:\n${duplicateVersions.joinToString("\n") { "Version $it is used multiple times" }}")
            }

            println("All migration filenames are valid and version numbers are unique.")
        }

    }
    check {
        dependsOn("checkFlywayMigrationNames")
    }
    jar {
        dependsOn(configurations.runtimeClasspath)
        archiveBaseName = "app"

        manifest {
            attributes["Main-Class"] = "no.nav.tiltakspenger.saksbehandling.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath
                .get()
                .joinToString(separator = " ") { file -> file.name }
        }
    }
}
