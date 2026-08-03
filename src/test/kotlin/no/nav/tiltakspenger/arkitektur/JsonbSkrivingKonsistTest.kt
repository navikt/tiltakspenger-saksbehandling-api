package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import no.nav.tiltakspenger.libs.konsist.assertIngenBrudd
import no.nav.tiltakspenger.libs.konsist.kildefiler
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Håndhever én skrivemåte for jsonb-parametre, og at ingen går utenom Jackson via [org.postgresql.util.PGobject].
 *
 * Reglene ligger lokalt her først.
 * Når mønsteret er bevist, hører de hjemme i `konsist-regler` i tiltakspenger-libs.
 */
class JsonbSkrivingKonsistTest {

    /**
     * Et jsonb-parameter skrives som `:navn::jsonb`, uten innpakning.
     *
     * Kotliquery sender parameteren som **tekst**, og det er casten som gjør jobben.
     * Skriver du `to_jsonb(:navn)` uten cast, får du hele json-dokumentet escapet ned i én json-streng — `to_jsonb('{"a":1}'::text)` gir `"{\"a\":1}"`, ikke `{"a": 1}`.
     * Det feiler ikke ved skriving; det feiler først når Jackson skal lese raden tilbake.
     *
     * Repoet hadde fire skrivemåter, og alle unntatt den bare formen så ut som innpakningen var det bærende leddet:
     * `to_jsonb(:navn::jsonb)`, `to_json(:navn::jsonb)` og `to_jsonb(:navn::json)`.
     * Alle tre var funksjonelt like — `to_jsonb` på noe som allerede er json eller jsonb er identiteten — men de skjuler at det er casten som betyr noe.
     * En som rydder bort `::jsonb` i tro på at `to_jsonb` tar seg av konverteringen, skriver stille escapede strenger i kolonnen.
     *
     * Regelen forbyr derfor enhver innpakning rundt et bind-parameter, og krever `::jsonb` framfor `::json`.
     * `to_jsonb(kolonne)` på en ekte kolonneverdi er noe helt annet og er fortsatt lov — det er kun `(` etterfulgt av `:` som fanges.
     */
    @Test
    fun `jsonb-parametre skrives som bar cast`() {
        assertIngenBrudd(
            databaselagsfiler().flatMap { (sti, tekst) ->
                (INNPAKKET_PARAMETER.findAll(tekst) + JSON_FRAMFOR_JSONB.findAll(tekst))
                    .map { "$sti: ${it.value.trim()}" }
            },
            "Skriv jsonb-parametre som `:navn::jsonb`. Det er casten som konverterer — `to_jsonb(:navn)` uten cast gir en escapet json-streng, og en innpakning rundt casten skjuler hvem som gjør jobben.",
        )
    }

    /**
     * `PGobject` er pgjdbc sin konvolutt for en verdi driveren ikke kjenner typen til.
     *
     * Den ble brukt to steder: en `toPGObject`-hjelper som tok `Any?` og kalte `objectMapper` rett fra repoet, og en lesing av den egendefinerte `periode`-typen.
     * Begge er borte.
     * Skrivestien går nå gjennom en navngitt `*DbJson`-type med `toDbJson()`/`fromDbJson`, og lesestien bruker `stringOrNull`.
     *
     * Regelen finnes fordi `toPGObject`-varianten var usynlig for konvensjonen om at rene mappinger skal ha en enhetstest som pinner json-en:
     * uten en navngitt type er det ingenting å pinne, og et nytt felt endrer da formatet på disk uten at noe slår ut.
     */
    @Test
    fun `ingen bruk av PGobject`() {
        assertIngenBrudd(
            databaselagsfiler()
                .filter { (_, tekst) -> "PGobject" in tekst }
                .map { (sti, _) -> "$sti: bruker PGobject" },
            "Serialiser til jsonb gjennom en navngitt `*DbJson`-type med `toDbJson()`/`fromDbJson`, og les med `stringOrNull`. Da kan mappingen pinnes i en enhetstest.",
        )
    }

    /**
     * Vakt mot en vakuøs grønn kjøring.
     *
     * Begge reglene over består trivielt hvis skanningen ikke finner noen filer, og et tomt scope er ikke hypotetisk:
     * `scopeFromProduction` leter etter en `.git`-*katalog*, og i et git-arbeidstre er `.git` en fil — da skanner den feil tre eller ingenting.
     */
    @Test
    fun `skanningen finner databaselaget`() {
        val jsonbStatements = databaselagsfiler().count { (_, tekst) -> "::jsonb" in tekst }

        assertIngenBrudd(
            listOfNotNull("fant $jsonbStatements filer med jsonb-parametre".takeIf { jsonbStatements < 10 }),
            "Skanningen fant nesten ingenting, så reglene over sier ingenting. Sjekk om scopet peker på riktig tre.",
        )
    }

    /** Produksjonsfilene, som sti og innhold. */
    private fun databaselagsfiler(): List<Pair<String, String>> =
        Konsist.scopeFromProduction().kildefiler()
            .filterNot { it.path.iEtArbeidstre() }
            .map { it.path to it.text }

    /**
     * Et git-arbeidstre under repo-rota er en egen utsjekk av det samme repoet.
     * Konsist walker inn i det, og regelen ville da rapportert brudd på filer som ikke hører til utsjekken vi kjører i.
     */
    private fun String.iEtArbeidstre(): Boolean {
        val relativ = runCatching { arbeidskatalog.relativize(Path.of(this)) }.getOrNull() ?: return false
        return relativ.any { segment -> segment.toString() in ARBEIDSTREKATALOGER }
    }

    private val arbeidskatalog: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()

    private companion object {
        val ARBEIDSTREKATALOGER = setOf(".worktrees", ".worktree")

        /**
         * `to_json(`/`to_jsonb(` rett foran et bind-parameter.
         * Fanger både innpakningene som er redundante og `to_jsonb(:navn)` uten cast, som er den farlige.
         */
        val INNPAKKET_PARAMETER = Regex("""to_jsonb?\(\s*:\w+""")

        /** `::json` der vi mener `::jsonb`. */
        val JSON_FRAMFOR_JSONB = Regex(""":\w+::json(?!b)""")
    }
}
