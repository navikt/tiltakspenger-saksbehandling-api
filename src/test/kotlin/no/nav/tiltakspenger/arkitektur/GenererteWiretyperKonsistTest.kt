package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.walk

/**
 * Holder de Avro-genererte wire-typene fra PDL inne i infrastrukturen.
 *
 * Klassene under `no.nav.person.pdl.*` genereres fra skjemaene i `src/main/avro/` og er PDLs kontrakt, ikke vår modell.
 * Lekker de inn i domenet, arver vi et skjema vi ikke eier: feltene er nullbare fordi Avro krever det, navnene er PDLs, og en skjemaendring hos dem slår rett inn i domenekoden vår.
 * Infrastrukturen skal oversette dem til våre egne typer i kanten, slik `TiltaksdeltakerService` gjør med sine Kafka-DTO-er.
 *
 * Navnerommene leses ut av skjemafilene i stedet for å stå i en liste her.
 * Et nytt Avro-skjema er da dekket uten at noen husker å oppdatere regelen — og `skanningen finner navnerommene` fanger det om utledningen slutter å virke.
 *
 * Regelen ligger lokalt her først.
 * Når mønsteret er bevist, hører den hjemme i `konsist-regler` i tiltakspenger-libs, ved siden av den generelle `InfraImport`.
 */
class GenererteWiretyperKonsistTest {

    /**
     * Filer utenfor `infra` som fortsatt kjenner de genererte typene.
     *
     * `LeesahConsumer.kt` og `AktorV2Consumer.kt` *skal* kjenne dem — de er kanten mot Kafka — men de ligger i `kafka/` i stedet for `infra/kafka/`.
     * Kuren er å flytte dem, ikke å gjøre unntak for konsumenter.
     *
     * `PersonhendelseService.kt` er et ekte brudd: den tar imot `LeesahPersonhendelse` og gjør både filtrering og mapping på PDLs type.
     * Oversettingen hører hjemme i konsumenten, slik at servicen kun ser vår egen `Personhendelse`.
     *
     * `IdenthendelseService.kt` og `Personident.kt` er samme brudd i identhendelse-vertikalen — sistnevnte er en domenetype som bygges rett fra `aktor.v2.Identifikator`.
     * De to kan ikke ryddes før vertikalen har en egen domenetype for identhendelser; i dag går `IdenthendelseDb` helt inn i servicen.
     */
    private val filerSomVenterPåOpprydding = setOf(
        "PersonhendelseService.kt",
        "LeesahConsumer.kt",
        "AktorV2Consumer.kt",
        "IdenthendelseService.kt",
        "Personident.kt",
    )

    @Test
    fun `kun infrastruktur kjenner de genererte avro-typene`() {
        assertIngenBrudd(
            produksjonsfilerUtenforInfra()
                .filterNot { it.filnavn in filerSomVenterPåOpprydding }
                .flatMap { fil ->
                    fil.genererteImporter().map { "${fil.path}: importerer $it" }
                },
            "Avro-typene fra PDL er et skjema vi ikke eier. Oversett dem til våre egne typer i konsumenten, og la resten av koden se vår modell.",
        )
    }

    /**
     * Ratchet-en: en whitelistet fil som ikke lenger bryter regelen, skal ut av whitelisten.
     * Uten denne ville en ryddet fil blitt liggende og stilltiende dekket over et nytt brudd senere.
     */
    @Test
    fun `whitelisten inneholder ingen filer som allerede er ryddet`() {
        val fortsattBrytende = produksjonsfilerUtenforInfra()
            .filter { it.genererteImporter().isNotEmpty() }
            .map { it.filnavn }
            .toSet()

        assertIngenBrudd(
            (filerSomVenterPåOpprydding - fortsattBrytende).map { "$it står i whitelisten, men importerer ingen genererte typer" },
            "Fjern filene fra whitelisten i GenererteWiretyperKonsistTest.",
        )
    }

    /**
     * Utledningen er regelens fundament: finner den ingen navnerom, ville regelen over passert tom.
     */
    @Test
    fun `skanningen finner navnerommene`() {
        val navnerom = avronavnerom()

        navnerom shouldContain "no.nav.person.pdl.leesah"
        navnerom shouldContain "no.nav.person.pdl.aktor.v2"
        (navnerom.size >= 3) shouldBe true
    }

    private data class Produksjonsfil(val path: String, val filnavn: String, val importer: List<String>)

    private fun Produksjonsfil.genererteImporter(): List<String> =
        importer.filter { import -> avronavnerom().any { navnerom -> import.startsWith("$navnerom.") } }

    private fun produksjonsfilerUtenforInfra(): List<Produksjonsfil> =
        Konsist.scopeFromProduction().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .filterNot { fil -> fil.packagee?.name.orEmpty().split('.').any { it in INFRASEGMENTER } }
            .map { Produksjonsfil(it.path, it.path.substringAfterLast('/'), it.imports.map { import -> import.name }) }

    /**
     * Navnerommene slik de er deklarert i Avro-skjemaene, f.eks. `@namespace("no.nav.person.pdl.leesah")`.
     */
    private fun avronavnerom(): Set<String> = navneromCache

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

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private val navneromCache: Set<String> by lazy {
        val avrokatalog = arbeidskatalog.resolve("src/main/avro")
        avrokatalog.walk()
            .filter { it.toString().endsWith(".avdl") || it.toString().endsWith(".avsc") }
            .flatMap { fil -> namespaceRegex.findAll(fil.readText()).map { it.groupValues[1] } }
            .toSet()
    }

    private val namespaceRegex = Regex("""[@"]namespace"?[(:]\s*"([^"]+)"""")

    private companion object {
        val INFRASEGMENTER = setOf("infra", "infrastruktur")
    }
}
