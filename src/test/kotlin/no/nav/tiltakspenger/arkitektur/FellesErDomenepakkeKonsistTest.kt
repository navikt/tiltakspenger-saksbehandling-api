package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import io.kotest.matchers.collections.shouldBeEmpty
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Håndhever arbeidsdelingen mellom domenepakken `felles` og infrastrukturpakken `infra`.
 *
 * De to ligger side om side rett under `no.nav.tiltakspenger.saksbehandling`, og delingen er lest ut av strukturen selv: `felles` har aldri hatt en `infra`-underpakke.
 * Domenetyper som deles på tvers av domenene bor i `felles`, mens infrastrukturen rundt dem — db-mapping, http, kafka — bor under `infra`.
 *
 * Uten en regel er dette lett å bryte uten at noen tar beslutningen.
 * En db-mapper for en `felles`-type ser ut til å høre hjemme ved siden av typen den mapper, og da vokser det fram en `felles/infra/`-pakke som ingen bestemte seg for.
 * Regelen fanger begge retningene: infrastruktur som flytter inn i `felles`, og `felles` som strekker seg ut i infrastrukturen.
 *
 * Regelen ligger lokalt her først.
 * Når mønsteret er bevist, hører den hjemme i `konsist-regler` i tiltakspenger-libs, ved siden av den generelle `InfraImport`.
 */
class FellesErDomenepakkeKonsistTest {

    /**
     * Filer i `felles` som fortsatt importerer infrastruktur.
     *
     * `Tilgangskontroll.kt` leser AD-rollene sine fra `infra.setup.Configuration`.
     * Å rydde det er en designendring — konfigurasjonen må inn i domenet i stedet for å hentes derfra — og ikke en mekanisk flytting, så den står her til den tas.
     */
    private val filerSomVenterPåOpprydding = setOf("Tilgangskontroll.kt")

    @Test
    fun `felles har ingen infra-underpakker`() {
        assertIngenBrudd(
            fellesfiler()
                .filter { it.pakke.harInfrasegment() }
                .map { "${it.file.path}: ligger i ${it.pakke}" },
            "Infrastruktur hører hjemme under `infra`, ikke i domenepakken `felles`. Mapping av en felles domenetype er infrastruktur, selv om typen den mapper er felles.",
        )
    }

    @Test
    fun `felles importerer ikke infrastruktur`() {
        assertIngenBrudd(
            fellesfiler()
                .filterNot { it.filnavn in filerSomVenterPåOpprydding }
                .flatMap { fil ->
                    fil.file.imports
                        .filter { it.name.harInfrasegment() }
                        .map { "${fil.file.path}: importerer ${it.name}" }
                },
            "`felles` er domenekode og skal ikke avhenge av infrastruktur. Snu avhengigheten: la infrastrukturen sende inn det domenet trenger.",
        )
    }

    /**
     * Ratchet-en: en whitelistet fil som ikke lenger bryter regelen, skal ut av whitelisten.
     * Uten denne ville en ryddet fil blitt liggende og stilltiende dekket over et nytt brudd senere.
     */
    @Test
    fun `whitelisten inneholder ingen filer som allerede er ryddet`() {
        val fortsattBrytende = fellesfiler()
            .filter { fil -> fil.file.imports.any { it.name.harInfrasegment() } }
            .map { it.filnavn }
            .toSet()

        (filerSomVenterPåOpprydding - fortsattBrytende).shouldBeEmpty()
    }

    private data class Fellesfil(val file: KoFileDeclaration, val filnavn: String, val pakke: String)

    /** Produksjonsfilene som ligger i `felles` eller en underpakke av den. */
    private fun fellesfiler(): List<Fellesfil> =
        Konsist.scopeFromProduction().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .map { Fellesfil(it, it.path.substringAfterLast('/'), it.packagee?.name.orEmpty()) }
            .filter { it.pakke == FELLESPAKKE || it.pakke.startsWith("$FELLESPAKKE.") }

    /**
     * True når minst ett punktum-separert segment navngir infrastruktur.
     *
     * `infra` er segmentet vi bruker selv.
     * `infrastruktur` tas med fordi libs bruker det (`libs.persistering.infrastruktur`), og et slikt importbehov i `felles` er like mye et brudd.
     */
    private fun String.harInfrasegment(): Boolean = split('.').any { it in INFRASEGMENTER }

    /**
     * Et git-arbeidstre som ligger under repo-rota er en egen utsjekk av det samme repoet.
     * Konsist walker inn i det, og regelen ville da rapportert brudd på filer som ikke hører til utsjekken vi kjører i.
     *
     * Sjekken er relativ til arbeidskatalogen, ikke absolutt.
     * Kjører du bygget *inne* i et arbeidstre, er arbeidskatalogen selve arbeidstreet, og da skal regelen kjøre som vanlig — det er kun arbeidstrær under oss som skal utelates.
     */
    private fun String.iEtArbeidstre(): Boolean {
        val relativ = runCatching { arbeidskatalog.relativize(Path.of(this)) }.getOrNull() ?: return false
        return relativ.any { segment -> segment.toString() in arbeidstrekataloger }
    }

    private val arbeidskatalog: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()

    private val arbeidstrekataloger = setOf(".worktrees", ".worktree")

    private companion object {
        const val FELLESPAKKE = "no.nav.tiltakspenger.saksbehandling.felles"
        val INFRASEGMENTER = setOf("infra", "infrastruktur")
    }
}
