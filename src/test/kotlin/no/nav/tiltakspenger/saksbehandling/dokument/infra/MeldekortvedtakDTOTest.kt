package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortvedtakDTOTest {

    // TODO - denne burde vel ha en assert?
    @Test
    fun `kan serialiseres`() = runTest {
        val meldekortvedtak = ObjectMother.meldekortvedtak()
        val tiltaksdeltakelser = listOf(ObjectMother.tiltaksdeltakelse())

        meldekortvedtak.toJsonRequest(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltakelser = Tiltaksdeltakelser(tiltaksdeltakelser),
            sammenlign = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
            false,
        )
    }

    @Test
    fun `mapper en string til riktig saksbehandlerDTO`() {
        runTest {
            "tp-sak".tilSaksbehandlerDto { "Navn" } shouldBe BrevMeldekortvedtakDTO.SaksbehandlerDTO.Automatisk
            "Z123456".tilSaksbehandlerDto { "Navn" } shouldBe BrevMeldekortvedtakDTO.SaksbehandlerDTO.Manuell("Navn")
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
