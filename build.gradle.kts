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
            val migrationDir = project.file("app/src/main/resources/db/migration")
            val invalidFiles = migrationDir.walk()
                .filter { it.isFile && it.extension == "sql" }
                .filterNot { it.name.matches(Regex("V[0-9]+__[\\w]+\\.sql")) }
                .map { it.name }
                .toList()

            if (invalidFiles.isNotEmpty()) {
                throw GradleException("Invalid migration filenames:\n${invalidFiles.joinToString("\n")}")
            } else {
                println("All migration filenames are valid.")
            }
        }
    }

    check {
        dependsOn("checkFlywayMigrationNames")
    }
}
