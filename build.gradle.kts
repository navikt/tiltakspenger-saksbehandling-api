import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlinxCoroutinesVersion = "1.11.0"
val kotestVersion = "6.2.4"
val felleslibVersion = "0.0.20260831193322"
val mockkVersion = "1.14.11"
val ktorVersion = "3.4.3"
val testContainersVersion = "2.0.5"
val confluentVersion = "8.1.1"
val avroVersion = "1.12.2"
val prometeusVersion = "1.17.1"
val jackson2Version = "2.22.2"
val lz4Version = "1.11.2"
// Samme versjon som `kafka` i tiltakspenger-libs sin versjonskatalog; se constraint-blokka for hvorfor den må være strict.
val kafkaVersion = "4.3.1"

// Avro-pluginen drar inn `avro-tools` på buildscript-classpathen, og derfra kommer
// `avro-mapred` → `avro-ipc-jetty` → Jetty 9.4, som er EOL og ikke får sikkerhetsfikser.
// Sammen med gammel `commons-lang3`, `avro-compiler` 1.12.0 og jackson 2.18.x sto det for
// 16 Dependabot-alerts med scope `development`.
// Ingenting av det havner i imaget, men støyen skjuler de reelle runtime-funnene i alert-lista.
//
// Vi ekskluderer `avro-ipc-jetty` og ikke hele `avro-tools`: schemaene våre er `.avdl` (Avro IDL),
// og kodegenereringen trenger `avro-idl` fra samme tre — uten den feiler `generateAvro` med
// NoClassDefFoundError på `org/apache/avro/idl/IdlReader`.
buildscript {
    configurations["classpath"].exclude(group = "org.apache.avro", module = "avro-ipc-jetty")
    dependencies {
        constraints {
            // Versjonene er skrevet ut fordi buildscript-blokka evalueres før script-valene finnes;
            // hold dem i sync med `avroVersion` og `jackson2Version`.
            // Kodeinjeksjon i Avros Java-SDK (GHSA-rp46-r563-jrc7).
            add("classpath", "org.apache.avro:avro-compiler:1.12.2")
            // Ukontrollert rekursjon på lange inndata (GHSA-j288-q9x7-2f5v).
            add("classpath", "org.apache.commons:commons-lang3:3.18.0")
            // Avro drar inn en gammel jackson-bom her. Buildscript-classpathen er en egen
            // konfigurasjon, så `implementation(platform(...))` i dependencies-blokka når den ikke.
            add("classpath", "com.fasterxml.jackson.core:jackson-core:2.22.2")
            add("classpath", "com.fasterxml.jackson.core:jackson-databind:2.22.2")
        }
    }
}

dependencies {
    // Lås versjonene på alle Kotlin-komponenter til samme versjon
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    // Lås alle io.netty:* til samme versjon. r2dbc-postgresql/reactor-netty (transitiv via
    // persistering-infrastruktur) drar inn netty 4.1.x, mens ktor-server-netty bruker 4.2.x.
    // Uten dette havner både netty-codec (4.1) og netty-codec-base (4.2) på classpath med
    // duplikate baseklasser (ByteToMessageDecoder m.fl.), som med `-cp lib/*` lastes i feil
    // rekkefølge og brekker HTTP-pipelinen.
    implementation(platform("io.netty:netty-bom:4.2.17.Final"))

    // Vår egen kode er på jackson3 (tools.jackson), men jackson 2 kommer inn transitivt via
    // Confluents kafka-avro-serializer (kafka-schema-registry-client avhenger av jackson-databind)
    // og drar med seg jackson-bom 2.20.0. Den har bl.a. to PolymorphicTypeValidator-omgåelser
    // (GHSA-rmj7-2vxq-3g9f, GHSA-j3rv-43j4-c7qm) og en SSRF via InetSocketAddress-deserialisering
    // (GHSA-hgj6-7826-r7m5). Vi styrer versjonen selv i stedet for å vente på at Confluent bumper -
    // samme versjon som `jackson2` i tiltakspenger-libs sin versjonskatalog.
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jackson2Version"))

    constraints {
        // Confluent publiserer sin egen fork av kafka-clients som `8.1.1-ccs`. Den taper ikke
        // konfliktoppløsningen mot Apache 4.3.1 fra libs:kafka - Gradle leser "8.1.1-ccs" som
        // høyere enn "4.3.1" - så uten `strictly` er det Confluent-forken som havner i imaget.
        // Den er bygd på Kafka 4.1 og drar inn den avviklede `org.lz4:lz4-java` 1.8.0, som har
        // både out-of-bounds-lesing (GHSA-vqf4-7m7x-wgfc) og en informasjonslekkasje i den trygge
        // dekomprimereren (GHSA-cmp6-m4wj-q63q) - sistnevnte uten fiks på de koordinatene.
        // Med Apache-versjonen kommer i stedet `at.yawk.lz4:lz4-java`, som vedlikeholdes.
        implementation("org.apache.kafka:kafka-clients") {
            version { strictly(kafkaVersion) }
        }
        // Apache kafka-clients drar inn lz4-java 1.10.2, der de native XXHash-implementasjonene
        // kan krasje JVM-en på ugyldige byte-intervaller (GHSA-xx22-p4ch-683r).
        implementation("at.yawk.lz4:lz4-java:$lz4Version")
    }

    implementation("com.github.navikt.tiltakspenger-libs:soknad-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    // TODO: Modulene er ikke tatt i bruk ennå — direktekall mot tiltakshistorikk er blokkert til tilgangs-PR-en i navikt/mulighetsrommet er merget.
    implementation("com.github.navikt.tiltakspenger-libs:tiltaksdeltakelse-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltaksdeltakelse-infrastruktur:$felleslibVersion")
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
    implementation("com.github.navikt.tiltakspenger-libs:kafka-avro:$felleslibVersion")
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
    implementation("org.flywaydb:flyway-database-postgresql:12.11.0")
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
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Delte arkitekturregler; drar inn konsist transitivt (api-avhengighet). Egen versjon inntil felleslibVersion bumpes.
    testImplementation("com.github.navikt.tiltakspenger-libs:konsist-regler:$felleslibVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
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
    testImplementation(testFixtures("com.github.navikt.tiltakspenger-libs:tiltaksdeltakelse-domene:$felleslibVersion"))
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-test-common:$felleslibVersion")
}
plugins {
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
    kotlin("jvm") version "2.4.10"
    id("com.diffplug.spotless") version "8.8.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
    application
}
application {
    mainClass.set("no.nav.tiltakspenger.saksbehandling.AppKt")
}
repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}
apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    // Fjerner ubrukte importer automatisk i spotlessApply, og feiler i spotlessCheck.
                    // Eksplisitt aktivert fordi default code style (intellij_idea) deaktiverer den.
                    "ktlint_standard_no-unused-imports" to "enabled",
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
        // Gradles default på 512 MB holder ikke for suiten vår: den døde på «Java heap space» rundt test 750-850 av 1229.
        maxHeapSize = "2g"
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // Både testklasser og testmetoder i samme klasse kjører parallelt; ingen tester kan dele muterbar tilstand.
        // Tester markert med @IsolatedDatabaseTest serialiseres via ResourceLock.
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
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
        // I en worktree er .git en fil (gitdir-peker), ikke en katalog; hooks eies av hovedklonen, så tasken hopper over.
        // Verdien fanges utenfor lambdaen: configuration cache kan ikke serialisere referanser til byggskript-objekter.
        val erHovedklone = file(".git").isDirectory
        onlyIf("kun i hovedklonen, ikke i worktrees") { erHovedklone }
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

// Klienter som er migrert til libs `httpklient` og skal ha full linjedekning.
// Utvid lista etter hvert som flere klienter migreres (jf. TASKS.md httpklient-punkt).
val httpklientKlasserMedDekningskrav =
    listOf(
        "no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaHttpClient",
        "no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinHttpClient",
        "no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient",
        "no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient",
        "no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenrsHttpClient",
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

// Hele databaselaget skal ha full grendekning (CoverageUnit.BRANCH), jf. testtaksonomien i AGENTS.md.
// Mønstre framfor navneliste: ny kode i databaselaget er dekket som standard, i stedet for å måtte legges til for hånd.
// En navneliste ville dessuten mistet dekningen stille ved en pakke- eller navneendring.
//
// Kover matcher på fullt klassenavn, og `*` dekker også punktum — det finnes ingen `**`, og det trengs ikke.
// Ett `*` spenner altså over vilkårlig mange pakkenivåer, og de to mønstrene er derfor så brede som de ser ut.
val databaselagMedDekningskrav =
    listOf(
        // Alt som ligger under en `infra/repo`-pakke, uansett hvor dypt: `<domene>/infra/repo/`, `<domene>/infra/repo/<mappe>/`, `infra/repo/`.
        // `*DbJson`-filene ligger alle her, og fanges av dette mønsteret.
        "no.nav.tiltakspenger.saksbehandling.*infra.repo.*",
        // Alt som heter `*Repo`, uansett hvor det ligger — i praksis portene, som bor sammen med domenekoden sin, men et repo som havner et annet sted er dekket uten at gaten må endres.
        // Suffikset tar med de syntetiske `$DefaultImpls`-broene som defaultargumenter på et interface genererer.
        "no.nav.tiltakspenger.saksbehandling.*Repo*",
    )

// Bootstrap som ligger i `infra/repo`, men ikke er databaselag: oppkoblingen mot Postgres og Flyway-oppsettet.
// De kjøres kun fra `ApplicationContext`, som selv står utenfor gaten, og testinfrastrukturen kobler seg opp med libs' `TestDatabaseManager` i stedet.
// Å dekke dem ville krevd et tredje skjema med alle migreringene på nytt, for å bevise en oppkobling de to eksisterende testskjemaene allerede beviser.
// TODO jah: vurder om de heller hører hjemme under `infra/setup` sammen med resten av bootstrappen.
val bootstrapUtenforDekningskravet =
    listOf(
        "no.nav.tiltakspenger.saksbehandling.infra.repo.DataSourceSetup*",
        "no.nav.tiltakspenger.saksbehandling.infra.repo.FlywayMigrateKt",
    )

// `*`-suffikset på httpklient-klassene tar med indre klasser og lambdaer; databaselag-mønstrene har det allerede.
val klasserMedFullDekningskrav = httpklientKlasserMedDekningskrav.map { "$it*" } + databaselagMedDekningskrav

kover {
    currentProject {
        instrumentation {
            // Instrumenter kun klassene dekningsgaten måler.
            // Agent på hele klassestien koster ellers rundt ti prosent av testtiden.
            includedClasses.addAll(klasserMedFullDekningskrav)
        }
    }
    reports {
        total {
            filters {
                includes {
                    classes(klasserMedFullDekningskrav)
                }
                excludes {
                    classes(bootstrapUtenforDekningskravet)
                }
            }
            verify {
                onCheck = true
                // De to gatene utfyller hverandre og må stå side om side.
                // Full grendekning sier ingenting om en linje uten grener, og full linjedekning sier ingenting om hvilken vei et vilkår ble tatt.
                rule("migrerte httpklient-klienter og hele databaselaget har full linjedekning") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
                rule("migrerte httpklient-klienter og hele databaselaget har full grendekning") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.BRANCH
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

// --- Ingen andre HTTP-klienter enn libs sin httpklient -------------------------
// Konsist-reglene (IngenAndreHttpKlienter) dekker det vi selv skriver og deklarerer.
// Denne dekker det siste hullet: en klient som kommer inn transitivt gjennom en annen
// avhengighet, uten at den står i noen import eller i denne fila.
//
// Ktor-klienten står bevisst IKKE på lista, og skal ikke legges til: `ktor-server-auth`
// eksponerer `ktor-client-core` som `api` (OAuth-provideren bruker den), så den ligger på
// både compile- og runtime-classpathen så lenge vi bruker ktor sin server-auth. Ktor-klienten
// håndheves derfor i kilden (konsist-regelen) og i byggfila, ikke her.
val verifiserHttpKlienter =
    tasks.register("verifiserHttpKlienter") {
        group = "verification"
        description = "Feiler hvis en annen HTTP-klient enn libs sin httpklient ligger på runtime-classpathen."
        // Lista ligger inne i tasken, ikke som script-val: configuration cache kan ikke
        // serialisere referanser til byggskript-objekter fanget i doLast.
        val forbudteHttpKlienter =
            listOf(
                "com.squareup.okhttp3",
                "com.squareup.retrofit2",
                // Apache HttpComponents står bevisst IKKE på lista i dette repoet, av samme grunn som
                // ktor-klienten: `io.confluent:kafka-avro-serializer` drar inn
                // `kafka-schema-registry-client`, som avhenger av `httpclient5` for oppslag mot
                // schema registry. Den ligger dermed på runtime-classpathen så lenge vi konsumerer
                // Avro-topics, uten at vi selv bruker den. Apache håndheves derfor i kilden
                // (konsist-regelen IngenAndreHttpKlienter), ikke her.
                "com.github.kittinunf.fuel",
                "com.konghq:unirest",
                "io.vertx:vertx-web-client",
                "org.http4k:http4k-client",
                "io.github.openfeign",
            )
        val artefakter = configurations.named("runtimeClasspath").get().incoming.artifacts
        // Filene som input gir Gradle task-avhengighetene: uten dem kan ikke artefaktene slås opp
        // før jar-taskene til et inkludert bygg har kjørt (composite build mot libs).
        inputs.files(artefakter.artifactFiles).withPropertyName("runtimeClasspath")
        val runtimeKomponenter =
            artefakter.resolvedArtifacts
                .map { liste -> liste.map { artefakt -> artefakt.id.componentIdentifier.displayName } }
        doLast {
            val funn = runtimeKomponenter.get().filter { komponent -> forbudteHttpKlienter.any { it in komponent } }
            if (funn.isNotEmpty()) {
                throw GradleException(
                    "Andre HTTP-klienter enn libs sin httpklient på runtime-classpathen:\n" +
                        funn.distinct().sorted().joinToString("\n") { "- $it" },
                )
            }
        }
    }

tasks.named("check") { dependsOn(verifiserHttpKlienter) }
