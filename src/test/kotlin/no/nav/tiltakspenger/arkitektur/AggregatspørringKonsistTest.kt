package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test

/**
 * Håndhever aggregat-disiplinen i testtaksonomien, jf. `AGENTS.md` og `../AGENTS-backend.md`.
 *
 * `hent*(limit)`-metodene på repo-portene finnes for jobbkøene: de velger ut rader på tvers av alle saker.
 * Brukes en slik spørring som lesekanal for én sak, testes verken utvalget, grensen eller sorteringen — testen består selv om spørringen plukker feil rader for alle andre saker.
 * Derfor skal de kun kalles fra `*AggregatTest`-filer, som asserter kontrakten deres direkte, og fra fake-repoene som implementerer dem.
 *
 * Begge reglene er en ratchet: whitelistene skal krympe for hver fil som ryddes i sveipen, og være tomme når den er ferdig.
 * `whitelistene inneholder ingen filer som allerede er ryddet` feiler så snart en fil er ren, slik at whitelisten ikke kan bli stående.
 *
 * Reglene ligger lokalt her først.
 * Når mønsteret er bevist, hører de hjemme i `konsist-regler` i tiltakspenger-libs.
 */
class AggregatspørringKonsistTest {

    /**
     * Filer som fortsatt kaller en `hent*(limit)`-metode utenfor en aggregat-test.
     *
     * `SendTilMeldekortApiServiceTest` tester livsløpet til `skal_sendes_til_meldekort_api`-flagget, ikke køspørringen.
     * Den står her fordi flagget ikke er lesbart per sak — verken `Sak` eller `SakDb` eksponerer det — så køen er eneste lesekanal.
     * Enten må saken eksponere flagget, eller så må testene bygges om sammen med resten av jobbtestene (tp-tax-5.3).
     */
    private val kallendeFilerSomVenterPåOpprydding = setOf(
        "SendTilMeldekortApiServiceTest.kt",
    )

    /** Filer som fortsatt sender `Int.MAX_VALUE` som limit. */
    private val maxValueFilerSomVenterPåOpprydding = setOf(
        "SendTilMeldekortApiServiceTest.kt",
    )

    @Test
    fun `hent-metoder med limit kalles kun fra aggregat-tester og fake-repoer`() {
        assertIngenBrudd(
            kallendeBrudd().filterNot { it.filnavn in kallendeFilerSomVenterPåOpprydding }.map { it.tekst },
            "Spørringer med limit velger ut på tvers av saker og hører hjemme i en *AggregatTest som asserter utvalg, limit og sortering (se testtaksonomien i AGENTS.md).",
        )
    }

    @Test
    fun `Int MAX_VALUE sendes ikke som limit til en repo-port`() {
        assertIngenBrudd(
            maxValueBrudd().filterNot { it.filnavn in maxValueFilerSomVenterPåOpprydding }.map { it.tekst },
            "limit = Int.MAX_VALUE slår av grensen spørringen finnes for, så den blir aldri testet (se testtaksonomien i AGENTS.md).",
        )
    }

    /**
     * Ratchet-en: en whitelistet fil som ikke lenger bryter regelen, skal ut av whitelisten.
     * Uten denne ville en ryddet fil blitt liggende og stilltiende dekket over et nytt brudd senere.
     */
    @Test
    fun `whitelistene inneholder ingen filer som allerede er ryddet`() {
        val fortsattKallende = kallendeBrudd().map { it.filnavn }.toSet()
        val fortsattMaxValue = maxValueBrudd().map { it.filnavn }.toSet()

        assertIngenBrudd(
            (kallendeFilerSomVenterPåOpprydding - fortsattKallende).map { "$it står i whitelisten for hent-metoder med limit, men kaller ingen" } +
                (maxValueFilerSomVenterPåOpprydding - fortsattMaxValue).map { "$it står i whitelisten for Int.MAX_VALUE, men bruker det ikke" },
            "Fjern filene fra whitelisten i AggregatspørringKonsistTest.",
        )
    }

    /**
     * Reglene utleder metodenavnene fra portene, slik at en ny jobbspørring dekkes uten at noen husker å oppdatere en liste.
     * Denne testen sikrer at utledningen faktisk finner noe: slår den feil, ville begge reglene over passert tomme.
     */
    @Test
    fun `portmetodene med limit utledes fra produksjonskoden`() {
        val metoder = portmetoderMedLimit()

        metoder shouldContain "hentDeSomSkalJournalføres"
        metoder shouldContain "hentSakerTilDatadeling"
        (metoder.size >= 20) shouldBe true
    }

    private data class Brudd(val filnavn: String, val tekst: String)

    private fun kallendeBrudd(): List<Brudd> {
        val metoder = portmetoderMedLimit()
        return testfiler()
            .filterNot { it.filnavn.endsWith("AggregatTest.kt") || it.filnavn.endsWith("FakeRepo.kt") }
            .flatMap { fil ->
                fil.kall(metoder).map { (linjenummer, metode) ->
                    Brudd(fil.filnavn, "${fil.file.path}:$linjenummer: kaller $metode utenfor en *AggregatTest")
                }
            }
    }

    private fun maxValueBrudd(): List<Brudd> {
        val metoder = portmetoderMedLimit()
        return testfiler()
            // Fake-repoene implementerer limit selv, og bruker Int.MAX_VALUE som «ingen grense» i sin egen kode.
            .filterNot { it.filnavn.endsWith("FakeRepo.kt") }
            .flatMap { fil ->
                fil.kall(metoder)
                    .filter { (_, _, kode) -> "Int.MAX_VALUE" in kode }
                    .map { (linjenummer, metode) ->
                        Brudd(fil.filnavn, "${fil.file.path}:$linjenummer: sender Int.MAX_VALUE som limit til $metode")
                    }
            }
    }

    private fun portmetoderMedLimit(): Set<String> =
        Konsist.scopeFromProduction().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .filter { "/ports/" in it.path }
            .flatMap { it.functions(includeNested = true) }
            .filter { funksjon -> funksjon.parameters.any { it.name == "limit" } }
            .map { it.name }
            .toSet()

    private fun testfiler(): List<Testfil> =
        Konsist.scopeFromTest().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .map { Testfil(it, it.path.substringAfterLast('/')) }

    /**
     * Et git-arbeidstre som ligger under repo-rota er en egen utsjekk av det samme repoet.
     * Konsist går inn i det, og reglene ville da rapportert brudd på filer som ikke er våre å rydde i.
     */
    private fun String.iEtArbeidstre(): Boolean = "/.worktrees/" in this || "/.worktree/" in this

    private data class Testfil(val file: KoFileDeclaration, val filnavn: String)

    /**
     * Kallene til [metoder] i fila, som (linjenummer, metodenavn, kodelinje).
     *
     * Tekstbasert med vilje: reglene ser etter kallsteder, og Konsists deklarasjons-API dekker deklarasjoner.
     * Kommentarlinjer hoppes over og strengliteraler maskeres, slik at tekst om et kall ikke teller som et kall.
     */
    private fun Testfil.kall(metoder: Set<String>): List<Triple<Int, String, String>> =
        file.text.lines().mapIndexedNotNull { indeks, linje ->
            val trimmet = linje.trim()
            if (trimmet.startsWith("//") || trimmet.startsWith("*") || trimmet.startsWith("/*")) {
                return@mapIndexedNotNull null
            }
            val kode = linje.replace(strengliteralRegex, "\"\"")
            if (mockkBlokkRegex.containsMatchIn(kode)) {
                return@mapIndexedNotNull null
            }
            metoder
                .firstOrNull { metode -> Regex("""\.$metode\s*\(""").containsMatchIn(kode) }
                ?.let { metode -> Triple(indeks + 1, metode, kode) }
        }

    private val strengliteralRegex = Regex(""""[^"]*"""")

    /**
     * En mockk-stub eller -verifisering nevner metoden, men kaller ikke spørringen.
     * Slike linjer er ikke et brudd på aggregat-disiplinen; at de finnes er et mock-problem som løses ved å bytte til en fake.
     */
    private val mockkBlokkRegex = Regex("""\b(every|verify|coEvery|coVerify)\s*[({]""")
}
