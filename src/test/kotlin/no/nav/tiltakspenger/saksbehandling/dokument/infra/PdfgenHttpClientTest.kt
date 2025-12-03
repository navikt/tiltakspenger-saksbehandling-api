package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class PdfgenHttpClientTest {

    // TODO - hva tester denne egentlig? :thinkies: - burde vel ha en assert?
    @Test
    fun genererMeldekortPdf() {
        runTest {
            val meldekortvedtak = ObjectMother.meldekortvedtak()
            PdfgenHttpClient("unused").genererMeldekortvedtakBrev(
                meldekortvedtak,
                tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
                sammenligning = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
                false,
            )
        }
    }

    @Test
    fun `genererer meldekort pdf fra command`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/api/v1/genpdf/tpts/utbetalingsvedtak"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/pdf"
                body = "ABCDEFGH"
            }

            val saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "4050")
            val fnr = Fnr.random()
            val meldekortId = MeldekortId.random()

            runTest {
                val actual = PdfgenHttpClient(wiremock.baseUrl()).genererMeldekortvedtakBrev(
                    command = GenererMeldekortVedtakBrevCommand(
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
                    ),
                    hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
                ).getOrFail()

                actual.json shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","meldekortPeriode":{"fom":"1. mai 2025","tom":"7. mai 2025"},"saksbehandler":{"navn":"Sak Behandler"},"beslutter":null,"tiltak":[],"iverksattTidspunkt":null,"fødselsnummer":"${fnr.verdi}","sammenligningAvBeregninger":{"meldeperioder":[],"totalDifferanse":0},"korrigering":false,"totaltBelop":50,"brevTekst":"Bacon ipsum dolor amet"}"""
            }
        }
    }

    private fun sammenlign(sammenligning: MeldeperiodeBeregning): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = sammenligning.periode,
            dager = emptyList(),
            differanseFraForrige = 0,
        )
    }
}
