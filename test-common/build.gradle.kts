val kotlinxCoroutinesVersion = "1.10.1"
val kotestVersion = "5.9.1"
val felleslibVersion = "0.0.345"

dependencies {
    api(project(":domene"))
    api(project(":app"))
    api("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:personklient-domene:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:personklient-infrastruktur:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:auth-test-core:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:auth-ktor:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    api("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    api("io.arrow-kt:arrow-core:1.2.4")
    api("io.kotest:kotest-assertions-core:$kotestVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    api("org.wiremock:wiremock:3.10.0")
    api("com.marcinziolo:kotlin-wiremock:2.1.1")
    api("io.kotest.extensions:kotest-extensions-wiremock:3.1.0")
    api("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
    api("io.github.serpro69:kotlin-faker:1.16.0")
}
