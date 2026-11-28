plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    application
}

base {
    archivesName.set("tiltakspenger-saksbehandling-api-lokal")
}

// `:lokal:run` starter LokalMain med fakes, mens imaget for docker-compose bruker LokalComposeMain (se Dockerfile).
application {
    applicationName = "tiltakspenger-saksbehandling-api-lokal"
    mainClass.set("no.nav.tiltakspenger.saksbehandling.lokal.LokalMainKt")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val felleslibVersion = rootProject.extra["felleslibVersion"] as String
val ktorVersion = rootProject.extra["ktorVersion"] as String

dependencies {
    // Låsen fra rota løser ikke versjonskonflikten når den bare kommer transitivt, så den må også stå her; se begrunnelsen i rotas byggfil.
    constraints {
        implementation("org.apache.kafka:kafka-clients") {
            version { strictly(rootProject.extra["kafkaVersion"] as String) }
        }
    }
    implementation(project(":"))
    // Ikke-transitiv, slik at bare dev-rutene kommer med og ikke en ekstra kopi av rota.
    implementation(project(":dev")) { isTransitive = false }
    // Fakes og testdata deles med testene i rota.
    implementation(testFixtures(project(":")))
    // Rota eksporterer ikke bibliotekene sine, så det lokalmodulen kompilerer mot står her.
    listOf(
        "common",
        "periodisering",
        "json",
        "kafka",
        "ktor-common",
        "logging",
        "persistering-domene",
        "persistering-infrastruktur",
        "soknad-dtos",
        "texas",
        "tiltak-dtos",
        "tiltaksdeltakelse-domene",
        "tiltaksdeltakelse-infrastruktur",
        "kafka-avro",
        "personklient-domene",
        "personklient-infrastruktur",
        "httpklient-infrastruktur",
    ).forEach { implementation("no.nav.tiltakspenger.libs:$it:$felleslibVersion") }
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("io.arrow-kt:arrow-core:2.2.3")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.1")
    implementation("org.flywaydb:flyway-database-postgresql:13.13.0")
    implementation("com.github.seratch:kotliquery:1.9.1")
    implementation("io.github.serpro69:kotlin-faker:1.16.2")
    implementation("no.nav.tiltakspenger.libs:test-common:$felleslibVersion") { isTransitive = false }
}

kotlin {
    jvmToolchain(25)
}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    "ktlint_standard_no-unused-imports" to "enabled",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                    "ktlint_standard_blank-line-before-declaration" to "enabled",
                ),
            )
    }
}
