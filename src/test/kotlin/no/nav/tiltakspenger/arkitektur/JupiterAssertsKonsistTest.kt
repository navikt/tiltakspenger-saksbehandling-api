package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test

class JupiterAssertsKonsistTest {
    @Test
    fun `testkode bruker Kotest assertions, ikke Jupiter Assertions`() {
        val violations =
            Konsist
                .scopeFromTest()
                .files
                .flatMap { file ->
                    file.imports
                        .filter { import ->
                            import.name == "org.junit.jupiter.api.Assertions" ||
                                import.name.startsWith("org.junit.jupiter.api.Assertions.") ||
                                import.name == "org.junit.jupiter.api.assertThrows" ||
                                import.name == "org.junit.jupiter.api.assertDoesNotThrow" ||
                                import.name == "org.junit.jupiter.api.assertNull" ||
                                import.name == "org.junit.jupiter.api.assertTrue" ||
                                import.name == "org.junit.jupiter.api.assertFalse" ||
                                import.name.startsWith("org.junit.jupiter.api.assert")
                        }
                        .map { import -> "${file.path}: ${import.name}" }
                }

        withClue(
            "Bruk Kotest assertions (io.kotest.matchers.* / io.kotest.assertions.*). " +
                "Følgende Jupiter Assertions-importer er ikke tillat:\n" +
                violations.joinToString("\n"),
        ) {
            violations.shouldBeEmpty()
        }
    }
}
