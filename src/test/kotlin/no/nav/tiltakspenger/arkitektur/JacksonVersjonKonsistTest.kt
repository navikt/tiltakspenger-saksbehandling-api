package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test

/**
 * Vi bruker Jackson 3 (`tools.jackson.*`). Jackson 2 (`com.fasterxml.jackson.*`) ligger fortsatt
 * på classpath transitivt via tredjeparts­libs, men skal ikke brukes direkte i produksjonskode.
 *
 * Eneste lovlige unntak er `com.fasterxml.jackson.annotation.*` — annotasjons-artefakten deles
 * mellom Jackson 2 og 3 og brukes også av Jackson 3.
 */
class JacksonVersjonKonsistTest {
    @Test
    fun `produksjonskode bruker Jackson 3 (tools_jackson), ikke Jackson 2 (com_fasterxml_jackson)`() {
        val violations =
            Konsist
                .scopeFromProduction()
                .files
                .flatMap { file ->
                    file.imports
                        .filter { import -> import.name.startsWith("com.fasterxml.jackson.") }
                        .filterNot { import -> import.name.startsWith("com.fasterxml.jackson.annotation.") }
                        .map { import -> "${file.path}: ${import.name}" }
                }

        withClue(
            "Bruk Jackson 3 (tools.jackson.*). Følgende Jackson 2-importer (com.fasterxml.jackson.*, " +
                "unntatt .annotation) er ikke tillatt i produksjonskode:\n" +
                violations.joinToString("\n"),
        ) {
            violations.shouldBeEmpty()
        }
    }
}
