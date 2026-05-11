plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Versjonen må holdes i synk med plugin-versjonen i prosjektets build.gradle.kts.
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.2")
}

