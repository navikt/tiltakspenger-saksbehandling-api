val kotlinxCoroutinesVersion = "1.9.0"
val kotestVersion = "5.9.1"
val felleslibVersion = "0.0.304"
val mockkVersion = "1.13.12"
val ktorVersion = "3.0.2"
val testContainersVersion = "1.20.4"
val poaoTilgangVersjon = "2024.11.26_08.36-ad014162ce23"
val iverksettVersjon = "1.0_20241213145703_7ff5f9c"

plugins {
    application
}

application {
    mainClass.set("no.nav.tiltakspenger.vedtak.AppKt")
}

tasks {
    jar {
        dependsOn(configurations.runtimeClasspath)

        manifest {
            attributes["Main-Class"] = "no.nav.tiltakspenger.vedtak.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath
                .get()
                .joinToString(separator = " ") { file -> file.name }
        }
    }
}

dependencies {
    implementation(project(":domene"))
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    implementation("com.github.navikt.tiltakspenger-libs:soknad-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:datadeling-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-ktor:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")

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

    implementation("com.natpryce:konfig:1.6.10.0")

    // Http
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")

    // Auth
    api("com.auth0:java-jwt:4.4.0")
    api("com.auth0:jwks-rsa:0.22.1")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:11.1.0")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    // Helved/Utsjekk/Utbetaling
    implementation("no.nav.utsjekk.kontrakter:iverksett:$iverksettVersjon")

    //POAO tilgang
    implementation("no.nav.poao-tilgang:client:$poaoTilgangVersjon")

    // DIV
    // Arrow
    implementation("io.arrow-kt:arrow-core:1.2.4")

    // Caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
    // need quarkus-junit-4-mock because of https://github.com/testcontainers/testcontainers-java/issues/970
    testImplementation("io.quarkus:quarkus-junit4-mock:3.17.4")
    testApi(project(":test-common"))
    testApi("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    testApi("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
}
