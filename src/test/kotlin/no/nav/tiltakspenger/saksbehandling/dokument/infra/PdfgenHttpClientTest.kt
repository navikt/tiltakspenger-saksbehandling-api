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

    private fun nyKlient(transport: FakeHttpTransport, isLocalOrDev: Boolean) = PdfgenHttpClient(
        baseUrl = "http://pdfgen",
        basePdfgenrsUrl = "http://pdfgenrs",
        isLocalOrDev = isLocalOrDev,
        clock = fixedClock,
        transport = transport,
    )

    private fun transportMedPdf(antallSvar: Int) = FakeHttpTransport().apply {
        repeat(antallSvar) { leggIKøBytes(pdfBytes, contentType = "application/pdf") }
    }

    /**
     * Kjører [kall] i prod-modus (kun pdfgen) og local/dev-modus (pdfgen + pdfgenrs i parallell) og asserter URI-ene som treffes.
     * Dekker dermed begge grenene av `isLocalOrDev` for metoden.
     */
    private fun verifiserBeggeModi(
        endepunkt: String,
        kall: suspend (PdfgenHttpClient) -> Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>>,
    ) = runTest {
        val prodTransport = transportMedPdf(antallSvar = 1)
        val prodResultat = kall(nyKlient(prodTransport, isLocalOrDev = false)).getOrFail()
        prodResultat.first.pdf.getContent().toList() shouldBe pdfBytes.toList()
        prodResultat.second shouldBe null
        prodTransport.mottatteKall.map { it.uri.toString() } shouldBe listOf("http://pdfgen/api/v1/genpdf/tpts/$endepunkt")

        val devTransport = transportMedPdf(antallSvar = 2)
        val devResultat = kall(nyKlient(devTransport, isLocalOrDev = true)).getOrFail()
        devResultat.second.shouldNotBeNull()
        // Parallelle kall mot FIFO-faken gir nondeterministisk rekkefølge, derfor set-sammenligning.
        devTransport.mottatteKall.map { it.uri.toString() }.toSet() shouldBe setOf(
            "http://pdfgen/api/v1/genpdf/tpts/$endepunkt",
            "http://pdfgenrs/api/v1/genpdf/tpts/$endepunkt",
        )
    }

    @Test
    fun `genererInnvilgetVedtakBrev for søknadsbehandling treffer vedtakInnvilgelse`() {
        val vedtak = ObjectMother.nyRammevedtakInnvilgelse()
        verifiserBeggeModi("vedtakInnvilgelse") {
            it.genererInnvilgetVedtakBrev(
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
        val behandling = ObjectMother.nyVedtattRevurderingInnvilgelse()
        val vedtak = ObjectMother.nyttRammevedtak(
            sakId = behandling.sakId,
            fnr = behandling.fnr,
            behandling = behandling,
            periode = behandling.innvilgelsesperioder!!.totalPeriode,
        )
        verifiserBeggeModi("revurderingInnvilgelse") {
            it.genererInnvilgetVedtakBrev(
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
        verifiserBeggeModi("vedtakInnvilgelse") {
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
        verifiserBeggeModi("revurderingInnvilgelse") {
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
    fun `genererMeldekortvedtakBrev for vedtak treffer utbetalingsvedtak`() {
        val meldekortvedtak = ObjectMother.meldekortvedtak(opprettet = nå(fixedClock))
        verifiserBeggeModi("utbetalingsvedtak") {
            it.genererMeldekortvedtakBrev(
                meldekortvedtak = meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                sammenligning = { sammenlign(it) },
            )
        }
    }

    @Test
    fun `genererMeldekortvedtakBrev for kommando treffer utbetalingsvedtak`() {
        verifiserBeggeModi("utbetalingsvedtak") {
            it.genererMeldekortvedtakBrev(
                kommando = meldekortvedtakBrevKommando(),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererMeldekortvedtakBrevV2 for vedtak treffer meldekortvedtak`() {
        val meldekortvedtak = ObjectMother.meldekortvedtak(opprettet = nå(fixedClock))
        verifiserBeggeModi("meldekortvedtak") {
            it.genererMeldekortvedtakBrevV2(
                meldekortvedtak = meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                sammenligning = { sammenlign(it) },
            )
        }
    }

    @Test
    fun `genererMeldekortvedtakBrevV2 for kommando treffer meldekortvedtak`() {
        verifiserBeggeModi("meldekortvedtak") {
            it.genererMeldekortvedtakBrevV2(
                kommando = meldekortvedtakBrevKommando(),
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
            )
        }
    }

    @Test
    fun `genererStansBrev treffer stansvedtak`() {
        val vedtak = ObjectMother.nyRammevedtakStans()
        verifiserBeggeModi("stansvedtak") {
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
        verifiserBeggeModi("stansvedtak") {
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
        verifiserBeggeModi("vedtakAvslag") {
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
        val vedtak = ObjectMother.nyRammevedtakAvslag()
        verifiserBeggeModi("vedtakAvslag") {
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
        verifiserBeggeModi("klageAvvis") {
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
        verifiserBeggeModi("klageInnstilling") {
            innstillingsbrev(it)
        }
    }

    @Test
    fun `bygger default transport når transport ikke sendes inn`() {
        PdfgenHttpClient(
            baseUrl = "http://pdfgen",
            basePdfgenrsUrl = "http://pdfgenrs",
            isLocalOrDev = false,
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
        verifiserBeggeModi("vedtakOpphør") {
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
        verifiserBeggeModi("vedtakOpphør") {
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

        innstillingsbrev(nyKlient(transport, isLocalOrDev = false)).getOrFail()

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
            val actual = nyKlient(transportMedPdf(antallSvar = 2), isLocalOrDev = true).genererMeldekortvedtakBrev(
                meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
                sammenligning = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
            ).getOrFail()

            actual.first.json shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","meldekortPeriode":{"fom":"6. januar 2025","tom":"19. januar 2025"},"saksbehandler":{"type":"MANUELL","navn":"Sak Behandler"},"beslutter":{"type":"MANUELL","navn":"Sak Behandler"},"tiltak":[{"tiltakstypenavn":"Arbeidsmarkedsoppfølging gruppe","tiltakstype":"GRUPPE_AMO"}],"iverksattTidspunkt":"1. januar 2025 01:02:03","fødselsnummer":"${fnr.verdi}","sammenligningAvBeregninger":{"meldeperioder":[{"tittel":"Meldekort 6. januar 2025 - 19. januar 2025","differanseFraForrige":0,"harBarnetillegg":false,"dager":[]}],"totalDifferanse":0},"korrigering":false,"totaltBelop":2980,"brevTekst":null,"forhandsvisning":false}"""
        }
    }

    @Test
    fun `genererer meldekort pdf fra command`() {
        val fnr = Fnr.random()
        val meldekortId = MeldekortId.random()
        val saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "4050")

        runTest {
            val actual = nyKlient(transportMedPdf(antallSvar = 2), isLocalOrDev = true).genererMeldekortvedtakBrev(
                kommando = meldekortvedtakBrevKommando(
                    saksnummer = saksnummer,
                    fnr = fnr,
                    meldekortId = meldekortId,
                ),
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
            ).getOrFail()

            actual.first.json shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","meldekortPeriode":{"fom":"1. mai 2025","tom":"7. mai 2025"},"saksbehandler":{"type":"MANUELL","navn":"Sak Behandler"},"beslutter":null,"tiltak":[],"iverksattTidspunkt":null,"fødselsnummer":"${fnr.verdi}","sammenligningAvBeregninger":{"meldeperioder":[],"totalDifferanse":0},"korrigering":false,"totaltBelop":50,"brevTekst":"Bacon ipsum dolor amet","forhandsvisning":true}"""
        }
    }

    @Test
    fun `feilstatus fra pdfgen gir KunneIkkeGenererePdf med PII-fri toString`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(500, body = "internal server error") }
        val fnr = Fnr.random()

        val resultat = nyKlient(transport, isLocalOrDev = false).genererInnstillingsbrev(fnr = fnr)

        val feil = resultat.swap().getOrNull().shouldNotBeNull()
        val httpFeil = feil.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        httpFeil.statusCode shouldBe 500
        // Payloaden inneholder fnr og skal kun til sikkerlogg via loggFeil, aldri ut gjennom toString.
        feil.toString() shouldNotContain fnr.verdi
        feil.toString() shouldBe "KunneIkkeGenererePdf(feil=UventetStatus, statusCode=500)"
    }

    @Test
    fun `feiler en av backendene i local dev feiler hele kallet`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøBytes(pdfBytes, contentType = "application/pdf")
            leggIKøStatus(503, body = "unavailable")
        }

        val resultat = nyKlient(transport, isLocalOrDev = true).genererInnstillingsbrev(fnr = Fnr.random())

        resultat.isLeft() shouldBe true
    }

    private suspend fun innstillingsbrev(klient: PdfgenHttpClient) = klient.genererInnstillingsbrev(fnr = Fnr.random())

    private suspend fun PdfgenHttpClient.genererInnstillingsbrev(fnr: Fnr): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> =
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
