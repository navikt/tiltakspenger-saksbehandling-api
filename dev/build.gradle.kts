import java.nio.file.Files
import java.util.zip.ZipFile

// Plugin-versjonene kommer fra rotprosjektets classpath.
plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlinx.kover")
    application
}

base {
    archivesName.set("tiltakspenger-saksbehandling-api-dev")
}

application {
    applicationName = "tiltakspenger-saksbehandling-api-dev"
    mainClass.set("no.nav.tiltakspenger.saksbehandling.dev.DevAppKt")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val felleslibVersion = rootProject.extra["felleslibVersion"] as String
val kotestVersion = rootProject.extra["kotestVersion"] as String
val ktorVersion = rootProject.extra["ktorVersion"] as String

dependencies {
    // Produksjonskoden i rota skal aldri avhenge av dev.
    implementation(project(":"))
    // Alt dev bruker, må allerede ligge i produksjonsdistribusjonen, og verifiserDevDistribusjon kontrollerer det.
    implementation("no.nav.tiltakspenger.libs:common:$felleslibVersion")
    implementation("no.nav.tiltakspenger.libs:ktor-common:$felleslibVersion")
    implementation("no.nav.tiltakspenger.libs:texas:$felleslibVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

tasks.test {
    useJUnitPlatform()
    // Configuration initialiseres én gang per JVM, så hver testklasse får sin egen.
    forkEvery = 1
    environment("NAIS_CLUSTER_NAME", "dev-gcp")
    // DevConfig leser påkrevde miljøvariabler når den initialiseres.
    listOf(
        "ROLE_SAKSBEHANDLER",
        "ROLE_BESLUTTER",
        "ROLE_VEILEDER",
        "ROLE_UTVIKLER",
        "ROLE_TILBAKEKREVING",
        "ELECTOR_PATH",
        "NAIS_APP_IMAGE",
        "DB_JDBC_URL",
        "NAIS_TOKEN_ENDPOINT",
        "NAIS_TOKEN_INTROSPECTION_ENDPOINT",
        "NAIS_TOKEN_EXCHANGE_ENDPOINT",
    ).forEach { environment(it, "ubrukt-i-test") }
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
                    // Fjerner ubrukte importer i spotlessApply og feiler i spotlessCheck.
                    "ktlint_standard_no-unused-imports" to "enabled",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                    // Krever blank linje før toppdeklarasjoner.
                    "ktlint_standard_blank-line-before-declaration" to "enabled",
                ),
            )
    }
}

val verifiserDevDistribusjon = tasks.register("verifiserDevDistribusjon") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Bygger begge distribusjonene og kontrollerer skillet mellom JAR-filene."
    val prodInstall = rootProject.tasks.named<Sync>("installDist")
    dependsOn(prodInstall, tasks.installDist)
    // Løses ved konfigurasjon for å støtte konfigurasjonscachen.
    val prodLib = prodInstall.get().destinationDir.resolve("lib")
    val prodJarNavn = rootProject.tasks.named<Jar>("jar").get().archiveFileName.get()
    val devLib = tasks.installDist.get().destinationDir.resolve("lib")
    val devJarNavn = tasks.jar.get().archiveFileName.get()
    val devKlasse = application.mainClass.get().replace('.', '/') + ".class"
    val spiFil = "META-INF/services/no.nav.tiltakspenger.saksbehandling.infra.setup.Miljøkonfigurasjonsleverandør"
    val fixturesJarNavn = rootProject.tasks.named<Jar>("testFixturesJar").get().archiveFileName.get()
    doLast {
        fun oppføringer(jar: File): Set<String> =
            ZipFile(jar).use { zip -> zip.entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toSet() }

        val prodJarer = prodLib.listFiles().orEmpty().filter { it.isFile }
        val prodNavn = prodJarer.map { it.name }.toSet()
        val devJarer = devLib.listFiles().orEmpty().filter { it.isFile }.associateBy { it.name }
        val devJar = devJarer[devJarNavn]
        check(prodJarNavn in prodNavn) { "Fant ikke $prodJarNavn i $prodLib" }
        check(devJar != null) { "Fant ikke $devJarNavn i $devLib" }
        check(devJarNavn !in prodNavn) { "Produksjonsdistribusjonen inneholder $devJarNavn." }
        check(devJarer.keys - devJarNavn == prodNavn) {
            "Dev-distribusjonen avviker fra produksjonsdistribusjonen: kun dev ${devJarer.keys - devJarNavn - prodNavn}, kun prod ${prodNavn - devJarer.keys}."
        }
        prodJarer.forEach { prod ->
            check(Files.mismatch(prod.toPath(), devJarer.getValue(prod.name).toPath()) == -1L) { "${prod.name} er ulik i dev- og produksjonsdistribusjonen." }
        }
        // De andre JAR-filene i dev er identiske med produksjon, så kontrollen under dekker begge distribusjonene.
        check(fixturesJarNavn !in prodNavn && fixturesJarNavn !in devJarer) { "En distribusjon inneholder $fixturesJarNavn." }
        fun kontrollerUtenLokal(jarNavn: String, innhold: Set<String>) {
            val lokale = innhold.filter {
                it.startsWith("no/nav/tiltakspenger/saksbehandling/lokal/") || it.startsWith("db/lokal/") || it == "logback.local.xml"
            }
            check(lokale.isEmpty()) { "$jarNavn inneholder filer for lokal kjøring: $lokale" }
        }

        val devOppføringer = oppføringer(devJar) - "META-INF/MANIFEST.MF"
        kontrollerUtenLokal(devJarNavn, devOppføringer)
        check(devKlasse in devOppføringer) { "$devJarNavn mangler inngangspunktet $devKlasse." }
        val devPakke = devKlasse.substringBeforeLast('/') + "/"
        check(spiFil in devOppføringer) { "$devJarNavn mangler $spiFil." }
        val leverandører = ZipFile(devJar).use { zip ->
            zip.getInputStream(zip.getEntry(spiFil)).bufferedReader().readLines()
        }.map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
        check(leverandører.size == 1) { "$spiFil må ha nøyaktig én leverandør, men har $leverandører." }
        val leverandørKlasse = leverandører.single().replace('.', '/') + ".class"
        check(leverandørKlasse.startsWith(devPakke) && leverandørKlasse in devOppføringer) {
            "Leverandøren ${leverandører.single()} ligger ikke i dev-pakken i $devJarNavn."
        }
        prodJarer.forEach { prod ->
            val prodOppføringer = oppføringer(prod)
            kontrollerUtenLokal(prod.name, prodOppføringer)
            check(prodOppføringer.none { it.startsWith(devPakke) }) { "${prod.name} inneholder dev-pakken $devPakke." }
            check(spiFil !in prodOppføringer) { "${prod.name} har en leverandør i $spiFil, og det skal bare dev ha." }
            val overlapp = devOppføringer intersect prodOppføringer
            check(overlapp.isEmpty()) { "$devJarNavn overlapper med ${prod.name}: $overlapp" }
        }
    }
}

tasks.check {
    dependsOn(verifiserDevDistribusjon)
}
