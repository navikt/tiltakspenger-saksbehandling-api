val kotlinxCoroutinesVersion = "1.10.1"
val kotestVersion = "5.9.1"
val felleslibVersion = "0.0.327"
val mockkVersion = "1.13.14"
val jacksonVersion = "2.18.2"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.aallam.ulid:ulid-kotlin:1.3.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion")

    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:tiltak-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:person-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:personklient-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")

    implementation("io.arrow-kt:arrow-core:1.2.4")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation(kotlin("test"))
    testImplementation(project(":test-common"))
    testApi("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}
