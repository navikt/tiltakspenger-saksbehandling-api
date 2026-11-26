import java.nio.file.Files
import java.util.zip.ZipFile

// Plugin-versjonene kommer fra rotprosjektets classpath.
plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
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

dependencies {
    // Rota skal aldri avhenge av dev.
    implementation(project(":"))
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
        val devOppføringer = oppføringer(devJar) - "META-INF/MANIFEST.MF"
        check(devKlasse in devOppføringer) { "$devJarNavn mangler inngangspunktet $devKlasse." }
        val devPakke = devKlasse.substringBeforeLast('/') + "/"
        prodJarer.forEach { prod ->
            val prodOppføringer = oppføringer(prod)
            check(prodOppføringer.none { it.startsWith(devPakke) }) { "${prod.name} inneholder dev-pakken $devPakke." }
            val overlapp = devOppføringer intersect prodOppføringer
            check(overlapp.isEmpty()) { "$devJarNavn overlapper med ${prod.name}: $overlapp" }
        }
    }
}

tasks.check {
    dependsOn(verifiserDevDistribusjon)
}
