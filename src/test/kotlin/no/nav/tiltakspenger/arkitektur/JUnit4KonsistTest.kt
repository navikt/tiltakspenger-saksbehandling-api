package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test

/**
 * Vi bruker JUnit 5 (Jupiter). JUnit 4 (`junit.framework.*` og `org.junit.*` utenom
 * `org.junit.jupiter.*` / `org.junit.platform.*`) skal ikke brukes i testkode.
 */
class JUnit4KonsistTest {
    @Test
    fun `testkode bruker JUnit 5, ikke JUnit 4`() {
        val violations =
            Konsist
                .scopeFromTest()
                .files
                .flatMap { file ->
                    file.imports
                        .filter { import ->
                            import.name.startsWith("junit.framework.") ||
                                (
                                    import.name.startsWith("org.junit.") &&
                                        !import.name.startsWith("org.junit.jupiter.") &&
                                        !import.name.startsWith("org.junit.platform.")
                                    )
                        }
                        .map { import -> "${file.path}: ${import.name}" }
                }

        withClue(
            "Bruk JUnit 5 (org.junit.jupiter.*). Følgende JUnit 4-importer er ikke tillatt:\n" +
                violations.joinToString("\n"),
        ) {
            violations.shouldBeEmpty()
        }
    }
}
