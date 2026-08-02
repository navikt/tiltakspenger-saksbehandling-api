package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import no.nav.tiltakspenger.libs.konsist.EnSetningPerLinje
import no.nav.tiltakspenger.libs.konsist.IngenAndreHttpKlienter
import no.nav.tiltakspenger.libs.konsist.IngenClockDefault
import no.nav.tiltakspenger.libs.konsist.IngenInternalModifier
import no.nav.tiltakspenger.libs.konsist.IngenJUnit4
import no.nav.tiltakspenger.libs.konsist.IngenJackson2
import no.nav.tiltakspenger.libs.konsist.IngenJupiterAsserts
import no.nav.tiltakspenger.libs.konsist.IngenLocalDateTimeNow
import no.nav.tiltakspenger.libs.konsist.IngenLokaleJacksonMappere
import no.nav.tiltakspenger.libs.konsist.IngenNowUtenClock
import no.nav.tiltakspenger.libs.konsist.IngenRewriteAudienceTarget
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Kjører de delte arkitekturreglene fra tiltakspenger-libs (`konsist-regler`) på dette repoet.
 *
 * Bevisst ikke aktivert ennå (målt 2026-07-17, se navikt/tiltakspenger#30):
 * `InfraImport` har 75 brudd og `BoundaryKlasser` 12 — lagdelingsgjeld som krever arkitekturbeslutninger, ikke mekanisk fiks.
 * `DomeneImportWhitelist` venter på de to over (75 distinkte importprefikser i domenet i dag).
 */
class FellesArkitekturKonsistTest {
    @Test
    fun `produksjonskode bruker Jackson 3, ikke Jackson 2`() {
        IngenJackson2.assert(Konsist.scopeFromProduction())
    }

    /**
     * `internal` avgrenser til kompileringsmodulen, og dette er et enmodul-repo som ikke publiseres.
     * Modifikatoren ville derfor sett ut som en tilgangsgrense uten å være en; `private` er den eneste grensen vi faktisk har her.
     */
    @Test
    fun `ingen internal-modifikator`() {
        IngenInternalModifier.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `testkode bruker JUnit 5, ikke JUnit 4`() {
        IngenJUnit4.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `testkode bruker Kotest assertions, ikke Jupiter Assertions`() {
        IngenJupiterAsserts.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `ingen lokale Jackson-mappere — bruk objectMapper fra libs-json`() {
        IngenLokaleJacksonMappere.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `henter aldri nåtid uten Clock`() {
        IngenNowUtenClock.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `bruk nå fra libs-common, ikke LocalDateTime-now`() {
        IngenLocalDateTimeNow.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `Clock-parametre har ikke default-verdi i produksjonskode`() {
        IngenClockDefault.assert(Konsist.scopeFromProduction())
    }

    /**
     * Produksjonskoden er fri for andre HTTP-klienter etter httpklient-migreringen, så regelen trenger ingen unntak her.
     */
    @Test
    fun `ingen andre http-klienter enn libs httpklient i produksjonskode`() {
        IngenAndreHttpKlienter.assertIngenKlienterIProduksjonskode(Konsist.scopeFromProduction())
    }

    /**
     * Testkoden får bruke `testApplication`-klienten, som kjører i minnet uten sokkel, men ikke lage ekte nettverksklienter.
     * Eksterne kall testes med produksjonsklienten over `FakeHttpTransport`, ikke med en klientmotor eller et fremmed klientbibliotek.
     */
    @Test
    fun `ingen ekte http-klienter i testkode`() {
        IngenAndreHttpKlienter.assertIngenKlienterITestkode(Konsist.scopeFromTest())
    }

    /** Fanger en klientavhengighet som er deklarert i byggfila, også før noen har tatt den i bruk. */
    @Test
    fun `ingen andre http-klienter deklarert i byggfila`() {
        IngenAndreHttpKlienter.assertIngenKlientavhengigheter(repoRot())
    }

    @Test
    fun `kdoc og kommentarer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `kdoc og kommentarer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `markdown-filer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIMarkdown(repoRot())
    }

    @Test
    fun `markdown-filer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIMarkdown(repoRot())
    }

    /**
     * `rewriteAudienceTarget` er utgått: `TexasHttpClient` utleder selv om target må skrives om, av formen på scope-verdien.
     * Feil kombinasjon av flagg og scope-verdi ga `invalid_scope` fra Entra ID og tok ned søknad-api i produksjon to ganger, andre gang fordi wiringen ble flyttet til en ny fil og flagget ble med på flyttelasset.
     * Regelen kjøres kun på produksjonskoden; test-fakes som implementerer `TexasClient` må beholde parameteret i signaturen så lenge det står i grensesnittet.
     */
    @Test
    fun `ingen bruk av det utgåtte rewriteAudienceTarget-flagget`() {
        IngenRewriteAudienceTarget.assert(Konsist.scopeFromProduction())
    }

    /** Enmodul-repo: testens arbeidskatalog er repo-rota. */
    private fun repoRot(): Path = Path.of(System.getProperty("user.dir"))
}
