package no.nav.tiltakspenger.saksbehandling.common

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Håndhever konvensjonen fra [IsolatedDatabaseTest]: alle tester som bruker `runIsolated = true` skal være annotert med `@IsolatedDatabaseTest`, slik at runneren serialiserer dem når parallellkjøring er aktivert.
 */
class IsolatedDatabaseTestKonvensjonTest {

    @Test
    fun `alle runIsolated-tester er annotert med IsolatedDatabaseTest`() {
        val testSourceRoot = File("src/test/kotlin")
        withClue("Fant ikke testkildekatalogen fra arbeidskatalogen ${File(".").absolutePath}") {
            testSourceRoot.isDirectory shouldBe true
        }

        val brudd = testSourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "IsolatedDatabaseTest.kt" && it.name != "IsolatedDatabaseTestKonvensjonTest.kt" }
            .flatMap { fil ->
                val linjer = fil.readLines()
                linjer.withIndex()
                    .filter { (_, linje) -> "runIsolated = true" in linje }
                    .mapNotNull { (indeks, _) ->
                        val testAnnotasjonIndeks = (indeks downTo 0).firstOrNull { linjer[it].trim() == "@Test" }
                            ?: return@mapNotNull "${fil.path}:${indeks + 1} bruker runIsolated = true utenfor en @Test-metode"
                        val harAnnotasjon = (testAnnotasjonIndeks..indeks).any { "@IsolatedDatabaseTest" in linjer[it] }
                        if (harAnnotasjon) {
                            null
                        } else {
                            "${fil.path}:${indeks + 1} bruker runIsolated = true uten @IsolatedDatabaseTest på testen"
                        }
                    }
            }
            .toList()

        brudd.shouldBeEmpty()
    }
}
