package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Håndhever repo-konvensjonen fra `AGENTS.md`.
 *
 * Et repo som nås fra en service skal nås gjennom et interface: grensesnittet `<Noe>Repo` i domenet, implementasjonen `<Noe>PostgresRepo` i `<domene>/infra/repo/`.
 * Uten interface kjenner domenet persisteringen, og testene må gå veien om databasen for å bytte den ut.
 *
 * Unntaket er repoer som aldri forlater infrastrukturen.
 * De kan stå uten interface, men må da være et `object` der hver offentlige funksjon tar inn en `Session` eller `TransactionalSession`.
 * De to typene er infrastruktur, så signaturen er selve vernet: domenet har ingen session å sende inn, og kan derfor ikke kalle repoet.
 *
 * Navnet `Repo` er samme kilde som Kover-gaten (`*Repo*`) og `AggregatspørringKonsistTest` bruker, så det er én konvensjon å bryte, ikke tre.
 *
 * Reglene ligger lokalt her først.
 * Når mønsteret er bevist, hører de hjemme i `konsist-regler` i tiltakspenger-libs.
 */
class RepoKonvensjonKonsistTest {

    /**
     * Begge whitelistene under peker på en TODO i fila, som sier hvorfor den står der og hva som skal til for å komme ut.
     * Begrunnelsen står der og ikke her, slik at den som åpner fila ser den — en whitelist ingen leser, er ingen arbeidsliste.
     * Ingen av oppføringene er ment å være permanente; en som er det, skal si det eksplisitt her i stedet for å peke på en TODO.
     *
     * De to reglene er bevisst ortogonale: navneregelen ser alle `*Repository`, interface-regelen kun de som alt heter `*Repo`.
     * `IdenthendelseRepository` står derfor kun i den første — omdøpingen er det som slipper den inn i den andre.
     */
    private val filerUtenInterfaceSomVenterPåOpprydding = setOf(
        // Se TODO i fila: porten venter på at `TiltaksdeltakerHendelse` blir en domenetype.
        "TiltaksdeltakerHendelsePostgresRepo.kt",
    )

    private val filerMedFeilSuffiksSomVenterPåOpprydding = setOf(
        // Se TODO i fila: omdøping og interface må tas sammen.
        "IdenthendelseRepository.kt",
    )

    @Test
    fun `repoer heter Repo, ikke Repository`() {
        assertIngenBrudd(
            feilSuffiks().filterNot { it.filnavn in filerMedFeilSuffiksSomVenterPåOpprydding }.map { it.tekst },
            "Suffikset er `Repo` for grensesnittet og `PostgresRepo` for implementasjonen.",
        )
    }

    @Test
    fun `en repo-klasse implementerer et Repo-grensesnitt`() {
        assertIngenBrudd(
            utenInterface().filterNot { it.filnavn in filerUtenInterfaceSomVenterPåOpprydding }.map { it.tekst },
            "Et repo som nås fra en service må ha et interface. Skal repoet aldri forlate infrastrukturen, gjør det til et `object` der hver offentlige funksjon tar inn en `Session` eller `TransactionalSession`.",
        )
    }

    @Test
    fun `et repo-object uten interface tar inn en session`() {
        assertIngenBrudd(
            objektUtenSession().map { it.tekst },
            "Uten interface er signaturen vernet: tar funksjonene inn en `Session` eller `TransactionalSession`, kan bare infrastrukturen kalle dem.",
        )
    }

    /**
     * Ratchet-en: en whitelistet fil som ikke lenger bryter regelen, skal ut av whitelisten.
     */
    @Test
    fun `whitelistene inneholder ingen filer som allerede er ryddet`() {
        val fortsattUtenInterface = utenInterface().map { it.filnavn }.toSet()
        val fortsattFeilSuffiks = feilSuffiks().map { it.filnavn }.toSet()

        assertIngenBrudd(
            (filerUtenInterfaceSomVenterPåOpprydding - fortsattUtenInterface).map { "$it står i whitelisten for manglende interface, men har et" } +
                (filerMedFeilSuffiksSomVenterPåOpprydding - fortsattFeilSuffiks).map { "$it står i whitelisten for feil suffiks, men har riktig" },
            "Fjern filene fra whitelisten i RepoKonvensjonKonsistTest.",
        )
    }

    /**
     * Utledningen er regelens fundament: finner den ingen repoer, ville reglene over passert tomme.
     */
    @Test
    fun `skanningen finner repoene`() {
        (repoklasser().size >= 15) shouldBe true
        (repoobjekter().isNotEmpty()) shouldBe true
    }

    private data class Brudd(val filnavn: String, val tekst: String)

    private fun feilSuffiks(): List<Brudd> =
        produksjonsfiler().flatMap { fil ->
            (fil.classes(includeNested = true).map { it.name } + fil.objects(includeNested = true).map { it.name } + fil.interfaces(includeNested = true).map { it.name })
                .filter { it.endsWith("Repository") }
                .map { Brudd(fil.path.substringAfterLast('/'), "${fil.path}: $it skal hete ${it.removeSuffix("Repository")}Repo") }
        }

    private fun utenInterface(): List<Brudd> =
        repoklasser()
            .filterNot { (_, klasse) -> klasse.parents().any { it.name.endsWith("Repo") } }
            .map { (path, klasse) -> Brudd(path.substringAfterLast('/'), "$path: ${klasse.name} er en klasse uten `Repo`-grensesnitt") }

    private fun objektUtenSession(): List<Brudd> =
        repoobjekter()
            .filterNot { (_, objekt) -> objekt.parents().any { it.name.endsWith("Repo") } }
            .flatMap { (path, objekt) ->
                objekt.functions()
                    .filterNot { it.hasPrivateModifier }
                    .filterNot { funksjon -> funksjon.parameters.any { it.type.name in SESJONSTYPER } }
                    .map { Brudd(path.substringAfterLast('/'), "$path: ${objekt.name}.${it.name} tar ikke inn en ${SESJONSTYPER.joinToString(" eller ")}") }
            }

    /** Klasser som er repo-implementasjoner: navnet ender på `Repo`, og de er ikke selve grensesnittet. */
    private fun repoklasser() =
        produksjonsfiler().flatMap { fil ->
            fil.classes(includeNested = true).filter { it.name.endsWith("Repo") }.map { fil.path to it }
        }

    private fun repoobjekter() =
        produksjonsfiler().flatMap { fil ->
            fil.objects(includeNested = true).filter { it.name.endsWith("Repo") }.map { fil.path to it }
        }

    private fun produksjonsfiler() =
        Konsist.scopeFromProduction().kildefiler().filterNot { it.path.iEtArbeidstre() }

    /**
     * Et git-arbeidstre som ligger under repo-rota er en egen utsjekk av det samme repoet.
     * Konsist walker inn i det, og reglene ville da rapportert brudd på filer som ikke hører til utsjekken vi kjører i.
     *
     * Sjekken er relativ til arbeidskatalogen, ikke absolutt.
     * Kjører du bygget *inne* i et arbeidstre, er arbeidskatalogen selve arbeidstreet, og da skal reglene kjøre som vanlig — det er kun arbeidstrær under oss som skal utelates.
     */
    private fun String.iEtArbeidstre(): Boolean {
        val relativ = runCatching { arbeidskatalog.relativize(Path.of(this)) }.getOrNull() ?: return false
        return relativ.any { segment -> segment.toString() in arbeidstrekataloger }
    }

    private val arbeidskatalog: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()

    private val arbeidstrekataloger = setOf(".worktrees", ".worktree")

    private companion object {
        val SESJONSTYPER = setOf("Session", "TransactionalSession")
    }
}
