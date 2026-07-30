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
            .filter { it.isFile && it.extension == "kt" && it.name != "IsolatedDatabaseTest.kt" && it.name != "IsolatedDatabaseTestKonvensjonTest.kt" && it.name != "TestDatabaseManager.kt" }
            .flatMap { fil ->
                val linjer = fil.readLines()
                linjer.withIndex()
                    // Kommentarer holdes utenfor: KDoc som forklarer når man skal bruke isolering er dokumentasjon, ikke et kall.
                    .filter { (_, linje) -> "runIsolated = true" in linje && !linje.erKommentar() }
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

    /**
     * Grov, men tilstrekkelig: regelen leser rå tekst, og en linje som starter som kommentar kan ikke inneholde et kall.
     */
    private fun String.erKommentar(): Boolean = trim().let {
        it.startsWith("//") || it.startsWith("*") || it.startsWith("/*")
    }
}
