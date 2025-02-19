import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmVersion = JvmTarget.JVM_21


plugins {
    kotlin("jvm") version "2.1.10"
    id("com.diffplug.spotless") version "7.0.2"
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://packages.confluent.io/maven/")
        maven {
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
    }
}

subprojects {
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
                    ),
                )
        }
    }

    tasks {
        kotlin {
            compilerOptions {
                jvmTarget.set(jvmVersion)
                freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            }
        }

        test {
            // JUnit 5 support
            useJUnitPlatform()
            // https://phauer.com/2018/best-practices-unit-testing-kotlin/
            systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
            // https://github.com/mockito/mockito/issues/3037#issuecomment-1588199599
            jvmArgs("-XX:+EnableDynamicAgentLoading")
            testLogging {
                // We only want to log failed and skipped tests when running Gradle.
                events("skipped", "failed")
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }

    configurations.all {
        // exclude JUnit 4
        exclude(group = "junit", module = "junit")
    }
}

tasks {
    register<Copy>("gitHooks") {
        from(file(".scripts/pre-commit"))
        into(file(".git/hooks"))
    }

    build {
        dependsOn("gitHooks")
    }

    register("checkFlywayMigrationNames") {
        doLast {
            val files = project.file("app/src/main/resources/db/migration").walk()
                .filter { it.isFile && it.extension == "sql" }
                .toList()

            val invalidFiles = files
                .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.sql")) }
                .map { it.name }

            if (invalidFiles.isNotEmpty()) {
                throw GradleException("Invalid migration filenames:\n${invalidFiles.joinToString("\n")}")
            }

            val duplicateVersions = files
                .mapNotNull { it.name.split("__").firstOrNull()?.removePrefix("V")?.toIntOrNull() }
                .groupBy { it }
                .filter { it.value.size > 1 }
                .keys

            if (duplicateVersions.isNotEmpty()) {
                throw GradleException("Duplicate version numbers found:\n${duplicateVersions.joinToString("\n") { "Version $it is used multiple times" }}")
            }

            println("All migration filenames are valid and version numbers are unique.")
        }
    }

    check {
        dependsOn("checkFlywayMigrationNames")
    }
}
