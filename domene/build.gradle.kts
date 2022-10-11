val jacksonVersion = "2.13.4"
val kotestVersion = "5.5.1"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.github.guepardoapps:kulid:2.0.0.0")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")

    testImplementation(kotlin("test"))
}
