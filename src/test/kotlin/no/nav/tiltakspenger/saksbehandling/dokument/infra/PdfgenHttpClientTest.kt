package no.nav.tiltakspenger.saksbehandling.dokument.infra

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class PdfgenHttpClientTest {

    @Test
    fun genererMeldekortPdf() {
        runTest {
            val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()
            PdfgenHttpClient("unused").genererUtbetalingsvedtak(
                utbetalingsvedtak,
                tiltaksdeltagelser = listOf(ObjectMother.tiltaksdeltagelse()),
                sammenligning = { sammenlign(utbetalingsvedtak.beregning.beregninger.first()) },
                hentSaksbehandlersNavn = { ObjectMother.saksbehandler().brukernavn },
            )
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
