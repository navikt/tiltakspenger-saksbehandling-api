package no.nav.tiltakspenger.saksbehandling.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingInnvilgelseTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.leggTilMeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test

class BeregnRevurderingTest {

    private val virkningsperiodeSøknadsbehandling = Periode(1.januar(2025), 30.juni(2025))
    private val virkningsperiodeRevurdering = virkningsperiodeSøknadsbehandling.plusTilOgMed(14)

    private val sats2025 = Satser.sats(1.januar(2025))

    private fun sakMedRevurdering(antallBarnFraSøknad: Int = 0): Pair<Sak, Revurdering> {
        val (sak) = nySakMedVedtak(
            virkningsperiode = virkningsperiodeSøknadsbehandling,
            barnetillegg = if (antallBarnFraSøknad > 0) {
                barnetillegg(
                    periode = virkningsperiodeSøknadsbehandling,
                    antallBarn = AntallBarn(antallBarnFraSøknad),
                )
            } else {
                null
            },
        ).first.genererMeldeperioder(fixedClock)

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            virkningsperiode = virkningsperiodeRevurdering,
        )

        return sak.copy(
            behandlinger = sak.behandlinger.leggTilRevurdering(revurdering),
        ) to revurdering
    }

    private fun tilBeslutningKommando(
        revurdering: Revurdering,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiodeRevurdering,
        ),
        barnetillegg: Barnetillegg? = null,
    ): RevurderingInnvilgelseTilBeslutningKommando {
        return RevurderingInnvilgelseTilBeslutningKommando(
            sakId = revurdering.sakId,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler(),
            correlationId = CorrelationId.generate(),
            begrunnelse = BegrunnelseVilkårsvurdering("lol"),
            fritekstTilVedtaksbrev = null,
            innvilgelsesperiode = virkningsperiodeRevurdering,
            tiltaksdeltakelser = revurdering.saksopplysninger.tiltaksdeltagelse.map {
                Pair(virkningsperiodeRevurdering, it.eksternDeltagelseId)
            },
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            barnetillegg = barnetillegg,
        )
    }

    @Test
    fun `Skal beregne utbetaling for revurdering når en legger til barn`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger, meldekortBehandling) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periode = virkningsperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
        )

        val beregning = sakMedMeldekortBehandlinger.beregnRevurderingInnvilgelse(kommando).getOrFail()

        beregning.shouldBeInstanceOf<BehandlingBeregning>()
        beregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        beregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8

        beregning.totalBeløp shouldBe meldekortBehandling.beregning.totalBeløp + beregning.barnetilleggBeløp
    }

    @Test
    fun `Skal ikke beregne en revurdering dersom ingen tidligere beregninger`() {
        val (sak, revurdering) = sakMedRevurdering()

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
        )

        sak.beregnRevurderingInnvilgelse(kommando).leftOrNull()
            .shouldBeInstanceOf<RevurderingIkkeBeregnet.IngenEndring>()
    }

    @Test
    fun `Skal ikke returnere beregning dersom det ikke er endringer i utbetaling`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
        )

        sakMedMeldekortBehandlinger.beregnRevurderingInnvilgelse(kommando).leftOrNull()
            .shouldBeInstanceOf<RevurderingIkkeBeregnet.IngenEndring>()
    }

    @Test
    fun `Skal ikke returnere beregning dersom det fører til tilbakekreving (fjerner barnetillegg)`() {
        val (sak, revurdering) = sakMedRevurdering(antallBarnFraSøknad = 1)

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
            barnetillegg = null,
        )

        sakMedMeldekortBehandlinger.beregnRevurderingInnvilgelse(kommando).leftOrNull()
            .shouldBeInstanceOf<RevurderingIkkeBeregnet.StøtterIkkeTilbakekreving>()
    }
}
