val ktorVersion = "2.2.3"
val kotestVersion = "5.5.5"
val mockkVersion = "1.13.4"
val testContainersVersion = "1.17.6"
val felleslibVersion = "0.0.28"

plugins {
    application
}

application {
    mainClass.set("no.nav.tiltakspenger.vedtak.AppKt")
}

dependencies {
    implementation(project(":domene"))
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    implementation("com.github.navikt.tiltakspenger-libs:arenatiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:arenaytelser-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:skjerming-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:fp-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ufore-dtos:$felleslibVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
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
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("com.github.navikt:rapids-and-rivers:2022122313141671797650.f806f770805a")
    implementation("com.natpryce:konfig:1.6.10.0")
    // Auth
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.auth0:jwks-rsa:0.22.0")

    // DB
    implementation("org.flywaydb:flyway-core:9.12.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
    // need quarkus-junit-4-mock because of https://github.com/testcontainers/testcontainers-java/issues/970
    testImplementation("io.quarkus:quarkus-junit4-mock:2.16.0.Final")
}
