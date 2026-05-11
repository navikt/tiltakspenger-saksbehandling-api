/**
 * Hjelpere for å lese Kovers (JaCoCo-kompatible) XML-rapport. Brukes både av
 * `koverVerifyTiers` og `koverMarkdownReport` i `kover-conventions.gradle.kts`.
 *
 * Vi går ikke via en full XML-parser fordi formatet er stabilt og smalt nok til at en
 * regex-parsing er enklere å lese, har null avhengigheter, og er trivielt å teste mot
 * eksempel-XML manuelt.
 */
object KoverReport {

    data class ClassCoverage(val name: String, val covered: Int, val missed: Int) {
        val total: Int get() = covered + missed
        val percent: Double get() = if (total == 0) 100.0 else 100.0 * covered / total
    }

    data class TotalCoverage(val covered: Int, val missed: Int) {
        val total: Int get() = covered + missed
        val percent: Double get() = if (total == 0) 100.0 else 100.0 * covered / total
    }

    private val classBlock = Regex(
        """<class name="([^"]+)"[^>]*>(.*?)</class>""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val lineCounter = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")

    /** Klasse-aggregert linjedekning, indeksert på fully qualified name (med `.`). */
    fun parseClasses(xml: String): Map<String, ClassCoverage> = classBlock.findAll(xml).associate { match ->
        val fqn = match.groupValues[1].replace('/', '.')
        val body = match.groupValues[2]
        // Klassens samlede LINE-counter er den siste LINE-counteren før </class>.
        val (missed, covered) = lineCounter.findAll(body).last().destructured
        fqn to ClassCoverage(fqn, covered.toInt(), missed.toInt())
    }

    /**
     * Total linjedekning for hele rapporten. Den siste LINE-counteren i dokumentet er rapportens
     * topplinje (ligger rett før `</report>` i Kovers/JaCoCos format).
     */
    fun parseTotal(xml: String): TotalCoverage {
        val last = lineCounter.findAll(xml).last()
        return TotalCoverage(
            covered = last.groupValues[2].toInt(),
            missed = last.groupValues[1].toInt(),
        )
    }

    /**
     * Matcher et tier-mønster mot kjente klasser. `*` matcher én pakke, `**` matcher rekursivt;
     * uten suffiks tolkes mønsteret som et eksakt klassenavn. Konvensjonen er den samme som
     * dokumentert i `kover-conventions.gradle.kts`.
     */
    fun matchPattern(pattern: String, classes: Map<String, ClassCoverage>): List<ClassCoverage> {
        val matcher: (String) -> Boolean = when {
            pattern.endsWith(".**") -> {
                val prefix = pattern.removeSuffix(".**")
                ({ fqn -> fqn == prefix || fqn.startsWith("$prefix.") })
            }
            pattern.endsWith(".*") -> {
                val prefix = pattern.removeSuffix(".*")
                ({ fqn ->
                    fqn.startsWith("$prefix.") &&
                        !fqn.removePrefix("$prefix.").contains('.')
                })
            }
            else -> ({ fqn -> fqn == pattern })
        }
        return classes.values.filter { matcher(it.name) }
    }
}

