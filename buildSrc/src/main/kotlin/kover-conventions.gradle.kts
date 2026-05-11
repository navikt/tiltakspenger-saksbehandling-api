import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

/**
 * Convention-plugin for Kover-konfig. Apply via `id("kover-conventions")`.
 *
 * Designprinsipper:
 *
 *  1. **Hele repoet får alltid en rapport.** `koverHtmlReport` og `koverXmlReport` kjøres
 *     automatisk på `check`. XML-en (JaCoCo-kompatibel) er klar til å mates inn i
 *     `koverMarkdownReport` (under), eller eksterne tjenester. HTML-en er ment lastet
 *     opp som build-artifact i CI.
 *
 *  2. **Generell baseline** – `koverVerify` enforcer at total linjedekning ikke faller under
 *     dagens nivå. Dette er en "safety net" som fanger store regresjoner. Oppdateres
 *     manuelt oppover når dekningen øker.
 *
 *  3. **Tier-baserte krav (100 / 75 / 50 %)** – `koverVerifyTiers` er en egen Gradle-task
 *     som leser XML-rapporten og verifiserer per-klasse / per-pakke krav. Vi bruker denne
 *     i stedet for Kover-varianter (`reports.variant("tier") { filters { … } verify { … } }`)
 *     fordi variant-filtre i Kover 0.9.x respekteres for `koverHtmlReport`/`koverXmlReport`,
 *     men *ikke* propageres ned til de tilhørende `koverVerifyVariant`-taskene. Inntil dette
 *     er fikset i Kover, er XML-parsing den mest forutsigbare måten å få per-tier
 *     enforcement på.
 *
 *  4. **`koverMarkdownReport`** – genererer en kompakt markdown-oppsummering (totaltall,
 *     baseline-status og tier-tabell) som workflow-en skriver til GitHub Step Summary
 *     og poster som sticky PR-kommentar. Holder oss uavhengige av tredjeparts-actions.
 *
 * Anbefaling om granularitet:
 *  Vi starter med klassenivå der det er praktisk (typisk én "ferskt testet" klasse av gangen),
 *  men det langsiktige målet er å løfte hele _pakker_ inn i tier-listene. Pakkenivå gir
 *  lavere vedlikeholdskostnad (ingen liste-pleie hver gang en ny klasse legges til), og
 *  signaliserer tydeligere kvalitetskrav til en hel modul. Pakkemønstre må slutte med ".*"
 *  (én pakke) eller ".**" (rekursivt), f.eks.
 *  `no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.**`.
 */

plugins {
    id("org.jetbrains.kotlinx.kover")
}

// ---------------------------------------------------------------------------
// Konfigurasjon – juster her, ikke i logikken under.
// ---------------------------------------------------------------------------

/** Sist målt: 73,49 % (mai 2026). Vi setter baseline 1 prosentpoeng under for å
 *  unngå falske positiver fra naturlig variasjon. Juster oppover når dekningen øker. */
val totalLineCoverageBaseline = 72

val tier100: List<String> = listOf(
    "no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiHttpClient",
)

val tier75: List<String> = listOf()

val tier50: List<String> = listOf()

private val tierMap: Map<Int, List<String>> = mapOf(
    100 to tier100,
    75 to tier75,
    50 to tier50,
).filterValues { it.isNotEmpty() }

// ---------------------------------------------------------------------------
// Native Kover-konfig: rapporter for hele repoet + baseline-verify.
// ---------------------------------------------------------------------------

kover {
    reports {
        total {
            verify {
                rule("Generell baseline – total linjedekning ≥ $totalLineCoverageBaseline %") {
                    groupBy = GroupingEntityType.APPLICATION
                    bound {
                        minValue = totalLineCoverageBaseline
                        coverageUnits = CoverageUnit.LINE
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// `koverVerifyTiers`: per-klasse / per-pakke verifisering basert på XML-rapporten.
// ---------------------------------------------------------------------------

abstract class KoverVerifyTiersTask : DefaultTask() {

    @get:InputFile
    abstract val report: RegularFileProperty

    @get:Input
    abstract val tiers: MapProperty<Int, List<String>>

    @TaskAction
    fun verify() {
        val xml = report.get().asFile.readText()
        val classes = KoverReport.parseClasses(xml)

        val violations = mutableListOf<String>()
        val summary = mutableListOf<String>()

        // Sorter høyeste tier først så loggen blir lettlest.
        tiers.get().toSortedMap(compareByDescending { it }).forEach { (threshold, patterns) ->
            patterns.forEach { pattern ->
                val matched = KoverReport.matchPattern(pattern, classes)
                if (matched.isEmpty()) {
                    violations += "Tier $threshold %: mønster '$pattern' matchet ingen klasser i rapporten."
                    return@forEach
                }
                matched.forEach { cls ->
                    val ok = cls.percent + 1e-9 >= threshold
                    val mark = if (ok) "✔" else "✘"
                    summary += "  $mark [$threshold %] ${cls.name} – ${"%.2f".format(cls.percent)} %"
                    if (!ok) {
                        violations += "Tier $threshold %: ${cls.name} har ${"%.2f".format(cls.percent)} % linjedekning."
                    }
                }
            }
        }

        if (summary.isNotEmpty()) {
            logger.lifecycle("Kover tier-sjekk:")
            summary.forEach { logger.lifecycle(it) }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "koverVerifyTiers feilet:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
    }
}

val koverVerifyTiers = tasks.register<KoverVerifyTiersTask>("koverVerifyTiers") {
    group = "verification"
    description = "Verifiserer tier-baserte linjedekningskrav (100/75/50 %) basert på Kovers XML-rapport."
    dependsOn(tasks.named("koverXmlReport"))
    report.set(layout.buildDirectory.file("reports/kover/report.xml"))
    tiers.set(tierMap)
}

// ---------------------------------------------------------------------------
// `koverMarkdownReport`: skriver en kompakt markdown-oppsummering som workflow-en
// klipper inn i GitHub Step Summary og bruker som body i PR-kommentaren.
// ---------------------------------------------------------------------------

abstract class KoverMarkdownReportTask : DefaultTask() {

    @get:InputFile
    abstract val report: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:Input
    abstract val baseline: Property<Int>

    @get:Input
    abstract val tiers: MapProperty<Int, List<String>>

    /** Sticky-tag som workflow-en bruker for å gjenfinne / oppdatere PR-kommentaren. */
    @get:Input
    abstract val commentTag: Property<String>

    @TaskAction
    fun generate() {
        val xml = report.get().asFile.readText()
        val total = KoverReport.parseTotal(xml)
        val classes = KoverReport.parseClasses(xml)
        val baselineValue = baseline.get()
        val baselineOk = total.percent + 1e-9 >= baselineValue

        val md = buildString {
            appendLine(commentTag.get())
            appendLine("## 📊 Kover coverage")
            appendLine()
            val mark = if (baselineOk) "✅" else "❌"
            appendLine(
                "**Total linjedekning:** **${"%.2f".format(total.percent)} %** " +
                    "(${total.covered} / ${total.total} linjer) — baseline ≥ $baselineValue % $mark",
            )
            appendLine()

            val t = tiers.get()
            if (t.isNotEmpty()) {
                appendLine("### Tier-krav")
                appendLine()
                appendLine("| Tier | Klasse / pakke | Dekning | Status |")
                appendLine("|------|----------------|---------|--------|")
                t.toSortedMap(compareByDescending { it }).forEach { (threshold, patterns) ->
                    patterns.forEach { pattern ->
                        val matched = KoverReport.matchPattern(pattern, classes)
                        if (matched.isEmpty()) {
                            appendLine("| $threshold % | `$pattern` | _ingen treff i rapporten_ | ⚠️ |")
                        } else {
                            matched.forEach { cls ->
                                val ok = cls.percent + 1e-9 >= threshold
                                val statusMark = if (ok) "✅" else "❌"
                                appendLine(
                                    "| $threshold % | `${cls.name}` | " +
                                        "${"%.2f".format(cls.percent)} % | $statusMark |",
                                )
                            }
                        }
                    }
                }
                appendLine()
            }
            appendLine(
                "<sub>Generert av <code>koverMarkdownReport</code>. " +
                    "Last ned full HTML-rapport under <em>Artifacts</em> på workflow-runen.</sub>",
            )
        }

        val out = output.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(md)
        logger.lifecycle("Coverage summary skrevet til ${out.absolutePath}")
    }
}

val koverMarkdownReport = tasks.register<KoverMarkdownReportTask>("koverMarkdownReport") {
    group = "verification"
    description = "Genererer en markdown-oppsummering av Kover-rapporten (til GitHub Step Summary / PR-kommentar)."
    dependsOn(tasks.named("koverXmlReport"))
    report.set(layout.buildDirectory.file("reports/kover/report.xml"))
    output.set(layout.buildDirectory.file("reports/kover/coverage-summary.md"))
    baseline.set(totalLineCoverageBaseline)
    tiers.set(tierMap)
    // Tag-en gjør at workflow-en finner og oppdaterer eksisterende PR-kommentar
    // i stedet for å spamme nye for hver push.
    commentTag.set("<!-- kover-coverage-comment -->")
}

// ---------------------------------------------------------------------------
// Hekt rapportering, tier-verify og markdown på `check`. `koverVerify` (baseline)
// hektes automatisk via Kovers innebygde wiring.
// ---------------------------------------------------------------------------

tasks.named("check") {
    dependsOn("koverHtmlReport", "koverXmlReport")
    dependsOn(koverVerifyTiers)
    dependsOn(koverMarkdownReport)
}

