package no.nav.tiltakspenger.saksbehandling.dokument.infra

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortvedtakDTOTest {

    @Test
    fun `kan serialiseres`() = runTest {
        val meldekortvedtak = ObjectMother.meldekortvedtak()
        val tiltaksdeltagelser = listOf(ObjectMother.tiltaksdeltagelse())

        meldekortvedtak.toJsonRequest(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltagelser = Tiltaksdeltagelser(tiltaksdeltagelser),
            sammenlign = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
        )
    }

    private fun sammenlign(sammenligning: MeldeperiodeBeregning): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = sammenligning.periode,
            dager = emptyList(),
            differanseFraForrige = 0,
        )
    }
}
