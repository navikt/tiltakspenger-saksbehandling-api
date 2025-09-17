package no.nav.tiltakspenger.saksbehandling.beregning

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.leggTilMeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test

class BeregnRevurderingTest {

    private val virkningsperiodeSøknadsbehandling = Periode(1.januar(2025), 30.juni(2025))
    private val virkningsperiodeRevurdering = virkningsperiodeSøknadsbehandling.plusTilOgMed(14)

    private val sats2025 = Satser.sats(1.januar(2025))

    private fun sakMedRevurdering(
        antallBarnFraSøknad: Int = 0,
        periodeForSøknadsbehandling: Periode = virkningsperiodeSøknadsbehandling,
        periodeForRevurdering: Periode = virkningsperiodeRevurdering,
        tiltakskodeForRevurdering: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
    ): Pair<Sak, Revurdering> {
        val (sak) = nySakMedVedtak(
            virkningsperiode = periodeForSøknadsbehandling,
            barnetillegg = if (antallBarnFraSøknad > 0) {
                barnetillegg(
                    periode = periodeForSøknadsbehandling,
                    antallBarn = AntallBarn(antallBarnFraSøknad),
                )
            } else {
                Barnetillegg.utenBarnetillegg(periodeForSøknadsbehandling)
            },
        ).first.genererMeldeperioder(fixedClock)

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            virkningsperiode = periodeForRevurdering,
            hentSaksopplysninger = {
                saksopplysninger(
                    fom = it.fraOgMed,
                    tom = it.tilOgMed,
                    tiltaksdeltagelse = tiltaksdeltagelse(
                        typeKode = tiltakskodeForRevurdering,
                        fom = it.fraOgMed,
                        tom = it.tilOgMed,
                    ),
                )
            },
        )

        return sak.copy(
            behandlinger = sak.behandlinger.leggTilRevurdering(revurdering),
        ) to revurdering
    }

    private fun tilBeslutningKommando(
        revurdering: Revurdering,
        innvilgelsesperiode: Periode = virkningsperiodeRevurdering,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            innvilgelsesperiode,
        ),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperiode),
    ): OppdaterRevurderingKommando.Innvilgelse {
        return OppdaterRevurderingKommando.Innvilgelse(
            sakId = revurdering.sakId,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler(),
            correlationId = CorrelationId.generate(),
            begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("lol"),
            fritekstTilVedtaksbrev = null,
            innvilgelsesperiode = innvilgelsesperiode,
            tiltaksdeltakelser = revurdering.saksopplysninger.tiltaksdeltagelser.map {
                Pair(innvilgelsesperiode, it.eksternDeltagelseId)
            },
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            barnetillegg = barnetillegg,
        )
    }

    @Test
    fun `Skal beregne etterbetaling for revurdering når en legger til barn`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger, meldekortBehandling) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val beløpFørRevurdering =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.beregnTotalBeløp()

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periode = virkningsperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
        )

        val nyBeregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        )

        nyBeregning.shouldNotBeNull()
        nyBeregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        nyBeregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8
        nyBeregning.totalBeløp shouldBe meldekortBehandling.beregning.totalBeløp + nyBeregning.barnetilleggBeløp

        nyBeregning.totalBeløp - beløpFørRevurdering shouldBe (sats2025.satsBarnetillegg * 8)
    }

    @Test
    fun `Skal beregne tilbakekreving for revurdering når en fjerner barn`() {
        val (sak, revurdering) = sakMedRevurdering(
            antallBarnFraSøknad = 2,
        )

        val (sakMedMeldekortBehandlinger, meldekortBehandling) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val beløpFørRevurdering =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.beregnTotalBeløp()

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periode = virkningsperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
        )

        val nyBeregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        )

        nyBeregning.shouldBeInstanceOf<BehandlingBeregning>()
        nyBeregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        nyBeregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8
        nyBeregning.totalBeløp shouldBe meldekortBehandling.beregning.totalBeløp - sats2025.satsBarnetillegg * 8

        nyBeregning.totalBeløp - beløpFørRevurdering shouldBe -(sats2025.satsBarnetillegg * 8)
    }

    @Test
    fun `Skal ikke beregne en revurdering dersom ingen tidligere beregninger`() {
        val (sak, revurdering) = sakMedRevurdering()

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
        )

        sak.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        ).shouldBeNull()
    }

    @Test
    fun `Skal ikke returnere ny beregning dersom det ikke er endringer i beregningen`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
        )

        sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        ).shouldBeNull()
    }

    @Test
    fun `Skal ikke returnere beregning dersom det kun er endring i tiltakstype`() {
        val (sak, revurdering) = sakMedRevurdering(
            tiltakskodeForRevurdering = TiltakstypeSomGirRett.ARBEIDSTRENING,
        )

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
        )

        sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        ).shouldBeNull()
    }

    @Test
    fun `Skal ikke endre beregningen for dager før eller etter revurderingsperioden`() {
        // Revurderer første meldeperiode, bortsett fra første og siste dag
        val førsteMeldeperiode = Periode(30.desember(2024), 12.januar(2025))
        val revurderingsperiode = førsteMeldeperiode.plusFraOgMed(1).minusTilOgMed(1)

        val (sak, revurdering) = sakMedRevurdering(
            antallBarnFraSøknad = 1,
            periodeForRevurdering = revurderingsperiode,
        )

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = førsteMeldeperiode,
        )

        val førsteDagIMeldeperioden =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.first().dager.first()
        val sisteDagIMeldeperioden =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.last().dager.last()

        val kommando = tilBeslutningKommando(
            revurdering = revurdering,
            innvilgelsesperiode = revurderingsperiode,
            barnetillegg = barnetillegg(
                periode = revurderingsperiode,
                antallBarn = AntallBarn(2),
            ),
        )

        val beregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            virkningsperiode = kommando.innvilgelsesperiode,
            barnetillegg = kommando.barnetillegg,
        )

        beregning.shouldNotBeNull()

        beregning.dager.find { it.dato == førsteDagIMeldeperioden.dato } shouldBe førsteDagIMeldeperioden
        beregning.dager.find { it.dato == sisteDagIMeldeperioden.dato } shouldBe sisteDagIMeldeperioden
    }
}
