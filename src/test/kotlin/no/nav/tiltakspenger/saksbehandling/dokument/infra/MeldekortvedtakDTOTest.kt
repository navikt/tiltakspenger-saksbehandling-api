package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldekortvedtakDTOTest {

    @Test
    fun `kan serialiseres`() = runTest {
        val saksnummer = Saksnummer.genererSaknummer(3.desember(2025), "4050")
        val fnr = Fnr.random()
        val meldekortId = MeldekortId.random()
        val meldekortvedtak = ObjectMother.meldekortvedtak(
            saksnummer = saksnummer,
            fnr = fnr,
            meldekortBehandling = ObjectMother.meldekortBehandletManuelt(
                id = meldekortId,
            ),
            opprettet = LocalDateTime.now(fixedClock),
        )
        val tiltaksdeltakelser = listOf(ObjectMother.tiltaksdeltakelse())

        meldekortvedtak.toJsonRequest(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltakelser = Tiltaksdeltakelser(tiltaksdeltakelser),
            sammenlign = { sammenlign(meldekortvedtak.utbetaling.beregning.beregninger.first()) },
            false,
        ) shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","meldekortPeriode":{"fom":"6. januar 2025","tom":"19. januar 2025"},"saksbehandler":{"type":"MANUELL","navn":"Saksbehandler Navn"},"beslutter":{"type":"MANUELL","navn":"Saksbehandler Navn"},"tiltak":[{"tiltakstypenavn":"Arbeidsmarkedsoppfølging gruppe","tiltakstype":"GRUPPE_AMO"}],"iverksattTidspunkt":"1. januar 2025 01:02:03","fødselsnummer":"${fnr.verdi}","sammenligningAvBeregninger":{"meldeperioder":[{"tittel":"Meldekort 6. januar 2025 - 19. januar 2025","differanseFraForrige":0,"harBarnetillegg":false,"dager":[]}],"totalDifferanse":0},"korrigering":false,"totaltBelop":2980,"brevTekst":null,"forhandsvisning":false}"""
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
