package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BrevMeldekortvedtakDTOTest {

    @Test
    fun `kan serialiseres`() = runTest {
        val saksnummer = Saksnummer.genererSaknummer(3.desember(2025), "4050")
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
        val tiltaksdeltakelser = listOf(ObjectMother.tiltaksdeltakelse())

        meldekortvedtak.tilBrevMeldekortvedtakJson(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltakelser = Tiltaksdeltakelser(tiltaksdeltakelser),
            sammenlign = { utenDager(it) },
        ) shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"erAutomatiskBehandlet":false,"saksbehandlerNavn":"Saksbehandler Navn","beslutterNavn":"Saksbehandler Navn","tiltak":["Arbeidsmarkedsoppfølging gruppe"],"iverksattTidspunkt":"1. januar 2025 01:02:03","fødselsnummer":"${fnr.verdi}","meldeperioder":[{"korrigering":false,"periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"beløp":2980,"beløpDiff":0,"harBarnetillegg":false,"dager":[]}],"totaltBelop":2980,"totalDifferanse":0,"brevTekst":null,"forhandsvisning":false}"""
    }

    @Test
    fun `mapper dagene i en meldeperiode`() = runTest {
        val saksnummer = Saksnummer.genererSaknummer(3.desember(2025), "4050")
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

        meldekortvedtak.tilBrevMeldekortvedtakJson(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltakelser = Tiltaksdeltakelser(listOf(ObjectMother.tiltaksdeltakelse())),
            sammenlign = { medEnDag(it) },
        ) shouldBe """{"meldekortId":"$meldekortId","saksnummer":"$saksnummer","periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"erAutomatiskBehandlet":false,"saksbehandlerNavn":"Saksbehandler Navn","beslutterNavn":"Saksbehandler Navn","tiltak":["Arbeidsmarkedsoppfølging gruppe"],"iverksattTidspunkt":"1. januar 2025 01:02:03","fødselsnummer":"${fnr.verdi}","meldeperioder":[{"korrigering":false,"periode":{"fraOgMed":"6. januar 2025","tilOgMed":"19. januar 2025"},"beløp":2980,"beløpDiff":900,"harBarnetillegg":true,"dager":[{"dato":"mandag 1. desember","status":{"forrige":"Deltatt","gjeldende":"Fravær godkjent av Nav","harEndring":true},"beløp":{"forrige":100,"gjeldende":1000,"harEndring":true},"barnetillegg":{"forrige":50,"gjeldende":500,"harEndring":true},"prosent":{"forrige":10,"gjeldende":50,"harEndring":true},"harEndring":true}]}],"totaltBelop":2980,"totalDifferanse":900,"brevTekst":null,"forhandsvisning":false}"""
    }

    private fun utenDager(beregning: MeldeperiodeBeregning): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = beregning.periode,
            dager = emptyList(),
            differanseFraForrige = 0,
        )
    }

    private fun medEnDag(beregning: MeldeperiodeBeregning): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = beregning.periode,
            dager = listOf(
                SammenligningAvBeregninger.DagSammenligning(
                    dato = 1.desember(2025),
                    status = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket.create(
                            dato = 1.desember(2025),
                            tiltakstype = TiltakstypeSomGirRettDTO.ARBEIDSFORBEREDENDE_TRENING,
                            antallBarn = AntallBarn(5),
                        ),
                        gjeldende = MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav.create(
                            dato = 1.desember(2025),
                            tiltakstype = TiltakstypeSomGirRettDTO.ARBEIDSTRENING,
                            antallBarn = AntallBarn(3),
                        ),
                    ),
                    beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = 100,
                        gjeldende = 1000,
                    ),
                    barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = 50,
                        gjeldende = 500,
                    ),
                    prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = 10,
                        gjeldende = 50,
                    ),
                ),
            ),
            differanseFraForrige = 900,
        )
    }
}
