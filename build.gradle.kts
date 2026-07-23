import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlinxCoroutinesVersion = "1.11.0"
val kotestVersion = "6.2.2"
val felleslibVersion = "0.0.20260723155827"
val mockkVersion = "1.14.11"
val ktorVersion = "3.4.3"
val testContainersVersion = "2.0.5"
val confluentVersion = "8.1.1"
val avroVersion = "1.12.1"
val prometeusVersion = "1.17.0"

dependencies {
    // Lås versjonene på alle Kotlin-komponenter til samme versjon
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    // Lås alle io.netty:* til samme versjon. r2dbc-postgresql/reactor-netty (transitiv via
    // persistering-infrastruktur) drar inn netty 4.1.x, mens ktor-server-netty bruker 4.2.x.
    // Uten dette havner både netty-codec (4.1) og netty-codec-base (4.2) på classpath med
    // duplikate baseklasser (ByteToMessageDecoder m.fl.), som med `-cp lib/*` lastes i feil
    // rekkefølge og brekker HTTP-pipelinen.
    implementation(platform("io.netty:netty-bom:4.2.16.Final"))

    implementation("com.github.navikt.tiltakspenger-libs:soknad-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:arenatiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:kafka:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:texas:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:satser:$felleslibVersion")

    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("com.aallam.ulid:ulid-kotlin:1.6.0")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    // Http
    implementation("io.ktor:ktor-http:$ktorVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:12.10.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Avro
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")

    // DIV
    // Arrow
    implementation("io.arrow-kt:arrow-core:2.2.3")
    implementation("io.arrow-kt:arrow-core-jackson:2.2.3")

    // Caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.10")
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Delte arkitekturregler; drar inn konsist transitivt (api-avhengighet). Egen versjon inntil felleslibVersion bumpes.
    testImplementation("com.github.navikt.tiltakspenger-libs:konsist-regler:$felleslibVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testContainersVersion")
    testImplementation("io.github.serpro69:kotlin-faker:1.16.2")
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:auth-test-core:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation(testFixtures("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion"))
    testImplementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-test-common:$felleslibVersion")
}
plugins {
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
    kotlin("jvm") version "2.4.10"
    id("com.diffplug.spotless") version "8.8.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
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
                    // Krev blank linje før topp-deklarasjoner (bl.a. etter siste import).
                    // Eksplisitt aktivert fordi default code style (intellij_idea) deaktiverer den.
                    "ktlint_standard_blank-line-before-declaration" to "enabled",
                ),
            )
    }
}
tasks {
    kotlin {
        jvmToolchain(25)
        compilerOptions {
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            freeCompilerArgs.add("-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled")
        }
    }

    test {
        // JUnit 5-støtte
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // https://github.com/mockito/mockito/issues/3037#issuecomment-1588199599
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            // Vi logger bare feilede og hoppede tester når Gradle kjører.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
configurations.all {
    // ekskluder JUnit 4
    exclude(group = "junit", module = "junit")
}
tasks {
    register<Copy>("gitHooks") {
        group = "git hooks"
        description = "Installerer git-hooks fra .gitHooks/ til .git/hooks/."
        from(file(".gitHooks"))
        into(file(".git/hooks"))
        filePermissions { unix("rwxr-xr-x") }
    }

    build {
        dependsOn("gitHooks")
    }

    register("checkFlywayMigrationNames") {
        val sqlMigrationDir = project.file("src/main/resources/db/migration")
        val kotlinMigrationDir = project.file("src/main/kotlin/db/migration")
        doLast {
            val sqlFiles =
                sqlMigrationDir
                    .walk()
                    .filter { it.isFile && it.extension == "sql" }
                    .toList()

            val invalidSqlFiles =
                sqlFiles
                    .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.sql")) }
                    .map { it.name }

            if (invalidSqlFiles.isNotEmpty()) {
                throw GradleException("Invalid SQL migration filenames:\n${invalidSqlFiles.joinToString("\n")}")
            }
            val kotlinFiles =
                kotlinMigrationDir
                    .walk()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    .toList()

            val invalidKotlinFiles =
                kotlinFiles
                    .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.(kt|java)")) }
                    .map { it.name }

            if (invalidKotlinFiles.isNotEmpty()) {
                throw GradleException("Invalid Kotlin/Java migration filenames:\n${invalidKotlinFiles.joinToString("\n")}")
            }

            // Sjekk for dupliserte versjoner på tvers av ALLE migreringstyper
            val allFiles = sqlFiles + kotlinFiles
            val duplicateVersions =
                allFiles
                    .mapNotNull {
                        it.name
                            .split("__")
                            .firstOrNull()
                            ?.removePrefix("V")
                            ?.toIntOrNull()
                    }.groupBy { it }
                    .filter { it.value.size > 1 }
                    .keys

            if (duplicateVersions.isNotEmpty()) {
                throw GradleException(
                    "Duplicate version numbers found:\n${duplicateVersions.joinToString("\n") { "Version $it is used multiple times" }}",
                )
            }

            println("All migration filenames are valid and version numbers are unique.")
        }
    }
    check {
        dependsOn("checkFlywayMigrationNames")
    }
}

kover {
    reports {
        total {
            filters {
                includes {
                    // Klienter som er migrert til libs `httpklient` og skal ha full linjedekning.
                    // Utvid lista etter hvert som flere klienter migreres (jf. TASKS.md httpklient-punkt).
                    classes(
                        "no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.KontorhistorikkHttpklient",
                        "no.nav.tiltakspenger.saksbehandling.saksbehandler.infra.MicrosoftGraphApiClient",
                        "no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveHttpClient",
                        "no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingHttpKlient",
                        "no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseHttpKlient",
                    )
                }
            }
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }
            verify {
                onCheck = true
                rule("migrerte httpklient-klienter har full linjedekning") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

tasks.named("koverXmlReport") {
    val xmlReport = layout.buildDirectory.file("reports/kover/report.xml")
    doLast {
        val xml = xmlReport.get().asFile
        val classCount = xml.readText().split("<class ").size - 1
        if (classCount == 0) throw GradleException("Kover report contains no classes — include filters likely stale")
    }
}
