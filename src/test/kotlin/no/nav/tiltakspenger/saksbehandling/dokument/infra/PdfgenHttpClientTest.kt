package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.Either
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.createOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test

/**
 * Tester klienten mot `FakeHttpTransport` slik at hele den reelle `HttpKlient`-pipelinen kjører (statusregel, Accept-header, binær dekoding, metadata).
 * Hver metode øves i begge modi: prod (kun pdfgen) og local/dev (pdfgen + pdfgenrs i parallell).
 */
internal class PdfgenHttpClientTest {

    // %PDF-magic etterfulgt av bytes som er ugyldige som UTF-8, slik at charset-dekoding underveis ville korruptert innholdet.
    private val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0xFF.toByte(), 0xFE.toByte())

    private val hentBrukersNavn: suspend (Fnr) -> Navn = { Navn("Fornavn", null, "Etternavn") }
    private val hentSaksbehandlersNavn: suspend (String) -> String = { "Sak Behandler" }
    private val saksnummer = Saksnummer.genererSaknummer(3.desember(2025), "4050")
    private val brevtekster = Brevtekster(listOf(TittelOgTekst("Tittel", "Tekst")))

    private fun nyKlient(transport: FakeHttpTransport) = PdfgenHttpClient(
        basePdfgenrsUrl = "http://pdfgenrs",
        clock = fixedClock,
        transport = transport,
    )

    private fun transportMedPdf(antallSvar: Int) = FakeHttpTransport().apply {
        repeat(antallSvar) { leggIKøBytes(pdfBytes, contentType = "application/pdf") }
    }

    /**
     * Kjører [kall] og asserter at kun pdfgenrs-endepunktet treffes, uavhengig av `isLocalOrDev`.
     * Brukes for metoder som utelukkende genererer brev via pdfgenrs.
     */
    private fun verifiserKunPdfgenrs(
        endepunkt: String,
        kall: suspend (PdfgenHttpClient) -> Either<KunneIkkeGenererePdf, PdfOgJson>,
    ) = runTest {
        val transport = transportMedPdf(antallSvar = 1)
        val resultat = kall(nyKlient(transport)).getOrFail()
        resultat.pdf.getContent().toList() shouldBe pdfBytes.toList()
        transport.mottatteKall.map { it.uri.toString() } shouldBe listOf("http://pdfgenrs/api/v1/genpdf/tpts/$endepunkt")
    }

    @Test
    fun `genererInnvilgetVedtakBrev for søknadsbehandling treffer vedtakInnvilgelse`() {
        verifiserKunPdfgenrs("vedtakInnvilgelse") { klient ->
            val vedtak = ObjectMother.nyRammevedtakInnvilgelse()
            klient.genererInnvilgetVedtakBrev(
                vedtak = vedtak,
                vedtaksdato = 2.januar(2023),
                tilleggstekst = null,
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererInnvilgetVedtakBrev for revurdering treffer revurderingInnvilgelse`() {
        verifiserKunPdfgenrs("revurderingInnvilgelse") { klient ->
            val behandling = ObjectMother.nyVedtattRevurderingInnvilgelse()
            val vedtak = ObjectMother.nyttRammevedtak(
                sakId = behandling.sakId,
                fnr = behandling.fnr,
                behandling = behandling,
                periode = behandling.innvilgelsesperioder!!.totalPeriode,
            )
            klient.genererInnvilgetVedtakBrev(
                vedtak = vedtak,
                vedtaksdato = 2.januar(2023),
                tilleggstekst = null,
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererInnvilgetSøknadBrevForhåndsvisning treffer vedtakInnvilgelse`() {
        verifiserKunPdfgenrs("vedtakInnvilgelse") {
            it.genererInnvilgetSøknadBrevForhåndsvisning(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                vedtaksdato = 2.januar(2025),
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = null,
                saksnummer = saksnummer,
                sakId = SakId.random(),
                innvilgelsesperioder = ObjectMother.innvilgelsesperioder(),
                barnetilleggsperioder = null,
                tilleggstekst = FritekstTilVedtaksbrev.createOrThrow("tilleggstekst"),
            )
        }
    }

    @Test
    fun `genererInnvilgetRevurderingBrevForhåndsvisning treffer revurderingInnvilgelse`() {
        verifiserKunPdfgenrs("revurderingInnvilgelse") {
            it.genererInnvilgetRevurderingBrevForhåndsvisning(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                vedtaksdato = 2.januar(2025),
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = null,
                saksnummer = saksnummer,
                sakId = SakId.random(),
                innvilgelsesperioder = ObjectMother.innvilgelsesperioder(),
                barnetilleggsperioder = null,
                tilleggstekst = FritekstTilVedtaksbrev.createOrThrow("tilleggstekst"),
            )
        }
    }

    @Test
    fun `genererMeldekortvedtakBrev for vedtak treffer meldekortvedtak`() {
        val meldekortvedtak = ObjectMother.meldekortvedtak(opprettet = nå(fixedClock))
        verifiserKunPdfgenrs("meldekortvedtak") {
            it.genererMeldekortvedtakBrev(
                meldekortvedtak = meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                sammenligning = { sammenlign(it) },
            )
        }
    }

    @Test
    fun `genererMeldekortvedtakBrev for kommando treffer meldekortvedtak`() {
        verifiserKunPdfgenrs("meldekortvedtak") {
            it.genererMeldekortvedtakBrev(
                kommando = meldekortvedtakBrevKommando(),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererStansBrev treffer stansvedtak`() {
        val vedtak = ObjectMother.nyRammevedtakStans()
        verifiserKunPdfgenrs("stansvedtak") {
            it.genererStansBrev(
                vedtak = vedtak,
                vedtaksdato = 2.januar(2023),
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harStansetBarnetillegg = false,
            )
        }
    }

    @Test
    fun `genererStansBrevForhåndsvisning treffer stansvedtak`() {
        verifiserKunPdfgenrs("stansvedtak") {
            it.genererStansBrevForhåndsvisning(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harStansetBarnetillegg = false,
                vedtaksdato = 2.januar(2025),
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = null,
                stansperiode = 1.januar(2025) til 31.januar(2025),
                saksnummer = saksnummer,
                sakId = SakId.random(),
                tilleggstekst = FritekstTilVedtaksbrev.createOrThrow("tilleggstekst"),
                valgteHjemler = nonEmptySetOf(HjemmelForStans.Alder),
            )
        }
    }

    @Test
    fun `genererAvslagsVedtaksbrev for parametre treffer vedtakAvslag`() {
        verifiserKunPdfgenrs("vedtakAvslag") {
            it.genererAvslagsVedtaksbrev(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = null,
                avslagsperiode = 1.januar(2025) til 31.januar(2025),
                saksnummer = saksnummer,
                sakId = SakId.random(),
                tilleggstekst = FritekstTilVedtaksbrev.createOrThrow("tilleggstekst"),
                forhåndsvisning = true,
                harSøktBarnetillegg = false,
                datoForUtsending = 2.januar(2025),
            )
        }
    }

    @Test
    fun `genererAvslagsVedtaksbrev for vedtak treffer vedtakAvslag`() {
        verifiserKunPdfgenrs("vedtakAvslag") {
            val vedtak = ObjectMother.nyRammevedtakAvslag()
            it.genererAvslagsVedtaksbrev(
                vedtak = vedtak,
                datoForUtsending = 2.januar(2023),
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererAvvisningsvedtak treffer klageAvvis`() {
        verifiserKunPdfgenrs("klageAvvis") {
            it.genererAvvisningsvedtak(
                saksnummer = saksnummer,
                fnr = Fnr.random(),
                tilleggstekst = brevtekster,
                saksbehandlerNavIdent = "Z123456",
                vedtaksdato = 2.januar(2025),
                forhåndsvisning = true,
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererInnstillingsbrev treffer klageInnstilling`() {
        verifiserKunPdfgenrs("klageInnstilling") {
            innstillingsbrev(it)
        }
    }

    @Test
    fun `bygger default transport når transport ikke sendes inn`() {
        PdfgenHttpClient(
            basePdfgenrsUrl = "http://pdfgenrs",
            clock = fixedClock,
        )
    }

    @Test
    fun `genererOpphørBrev treffer vedtakOpphør`() {
        val omgjøring = ObjectMother.nyIverksattRevurderingOmgjøring() as Revurdering
        val gammeltResultat = omgjøring.resultat as Omgjøringsresultat
        val opphørBehandling = omgjøring.copy(
            resultat = Omgjøringsresultat.OmgjøringOpphør(
                vedtaksperiode = gammeltResultat.vedtaksperiode!!,
                omgjørRammevedtak = gammeltResultat.omgjørRammevedtak,
                valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Introduksjonsprogrammet),
            ),
        )
        val vedtak = ObjectMother.nyttRammevedtak(
            sakId = opphørBehandling.sakId,
            fnr = opphørBehandling.fnr,
            behandling = opphørBehandling,
            periode = gammeltResultat.vedtaksperiode!!,
        )
        verifiserKunPdfgenrs("vedtakOpphør") {
            it.genererOpphørBrev(
                vedtak = vedtak,
                vedtaksdato = 2.januar(2025),
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harOpphørtBarnetillegg = false,
            )
        }
    }

    @Test
    fun `genererOpphørBrevForhåndsvisning treffer vedtakOpphør`() {
        verifiserKunPdfgenrs("vedtakOpphør") {
            it.genererOpphørBrevForhåndsvisning(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harOpphørtBarnetillegg = false,
                vedtaksdato = 2.januar(2025),
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = null,
                saksnummer = saksnummer,
                sakId = SakId.random(),
                tilleggstekst = FritekstTilVedtaksbrev.createOrThrow("tilleggstekst"),
                valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Introduksjonsprogrammet),
                vedtaksperiode = 1.januar(2025) til 31.januar(2025),
            )
        }
    }

    @Test
    fun `sender payloaden som JSON og aksepterer PDF`() = runTest {
        val transport = transportMedPdf(antallSvar = 1)

        innstillingsbrev(nyKlient(transport)).getOrFail()

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.request.headers().firstValue("Accept").get() shouldBe "application/pdf"
        kall.request.headers().firstValue("Content-Type").get() shouldBe "application/json"
    }

    @Test
    fun genererMeldekortPdf() {
        val fnr = Fnr.random()
        val meldekortId = MeldekortId.random()
        val meldekortvedtak = ObjectMother.meldekortvedtak(
            saksnummer = saksnummer,
            fnr = fnr,
            meldekortbehandling = ObjectMother.meldekortBehandletManuelt(
                id = meldekortId,
            ),
            opprettet = nå(fixedClock),
        )
        runTest {
            val actual = nyKlient(transportMedPdf(antallSvar = 1)).genererMeldekortvedtakBrev(
                meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
                sammenligning = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
            ).getOrFail()

            actual.json shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"erAutomatiskBehandlet":false,"saksbehandlerNavn":"Sak Behandler","beslutterNavn":"Sak Behandler","tiltak":["Arbeidsmarkedsoppfølging gruppe"],"iverksattTidspunkt":"1. januar 2025 01:02:03","fødselsnummer":"${fnr.verdi}","meldeperioder":[{"korrigering":false,"periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"beløp":2980,"beløpDiff":0,"harBarnetillegg":false,"dager":[]}],"totaltBelop":2980,"totalDifferanse":0,"brevTekst":null,"forhandsvisning":false}"""
        }
    }

    @Test
    fun `genererer meldekort pdf fra command`() {
        val fnr = Fnr.random()
        val meldekortId = MeldekortId.random()
        val saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "4050")

        runTest {
            val actual = nyKlient(transportMedPdf(antallSvar = 1)).genererMeldekortvedtakBrev(
                kommando = meldekortvedtakBrevKommando(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    meldekortId = meldekortId,
                ),
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
            ).getOrFail()

            actual.json shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","periode":{"fraOgMed":"1. mai 2025","tilOgMed":"7. mai 2025"},"erAutomatiskBehandlet":false,"saksbehandlerNavn":"Sak Behandler","beslutterNavn":null,"tiltak":[],"iverksattTidspunkt":null,"fødselsnummer":"${fnr.verdi}","meldeperioder":[],"totaltBelop":50,"totalDifferanse":0,"brevTekst":"Bacon ipsum dolor amet","forhandsvisning":true}"""
        }
    }

    @Test
    fun `feilstatus fra pdfgen gir KunneIkkeGenererePdf med PII-fri toString`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(500, body = "internal server error") }
        val fnr = Fnr.random()

        val resultat = nyKlient(transport).genererInnstillingsbrev(fnr = fnr)

        val feil = resultat.swap().getOrNull().shouldNotBeNull()
        val httpFeil = feil.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        httpFeil.statusCode shouldBe 500
        // Payloaden inneholder fnr og skal kun til sikkerlogg via loggFeil, aldri ut gjennom toString.
        feil.toString() shouldNotContain fnr.verdi
        feil.toString() shouldBe "KunneIkkeGenererePdf(feil=UventetStatus, statusCode=500)"
    }

    private suspend fun innstillingsbrev(klient: PdfgenHttpClient) = klient.genererInnstillingsbrev(fnr = Fnr.random())

    private suspend fun PdfgenHttpClient.genererInnstillingsbrev(fnr: Fnr): Either<KunneIkkeGenererePdf, PdfOgJson> =
        genererInnstillingsbrev(
            saksnummer = saksnummer,
            fnr = fnr,
            tilleggstekst = brevtekster,
            saksbehandlerNavIdent = "Z123456",
            forhåndsvisning = true,
            vedtaksdato = 2.januar(2025),
            hentBrukersNavn = hentBrukersNavn,
            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            innsendingsdato = 1.januar(2025),
            clock = fixedClock,
        )

    private fun meldekortvedtakBrevKommando(
        saksnummer: Saksnummer = this.saksnummer,
        fnr: Fnr = Fnr.random(),
        meldekortId: MeldekortId = MeldekortId.random(),
    ) = GenererMeldekortvedtakBrevKommando(
        sakId = SakId.random(),
        saksnummer = saksnummer,
        fnr = fnr,
        saksbehandler = "saksbehandler",
        beslutter = null,
        meldekortbehandlingId = meldekortId,
        beregningsperiode = Periode(1.mai(2025), 7.mai(2025)),
        tiltaksdeltakelser = Tiltaksdeltakelser(emptyList()),
        iverksattTidspunkt = null,
        erKorrigering = false,
        beregninger = listOf(),
        totaltBeløp = 50,
        tekstTilVedtaksbrev = NonBlankString.create("Bacon ipsum dolor amet"),
        forhåndsvisning = true,
    )

    private fun sammenlign(sammenligning: MeldeperiodeBeregning): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = sammenligning.periode,
            dager = emptyList(),
            differanseFraForrige = 0,
        )
    }
}
