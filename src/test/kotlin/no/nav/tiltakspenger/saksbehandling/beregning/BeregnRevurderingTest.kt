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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingInnvilgelseTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test

class BeregnRevurderingTest {
    private val virkningsperiodeSøknadsbehandling = Periode(1.januar(2025), 30.juni(2025))
    private val virkningsperiodeRevurdering = virkningsperiodeSøknadsbehandling.plusTilOgMed(14)

    private val sats2025 = Satser.sats(1.januar(2025))

    @Test
    fun `Skal beregne utbetaling for revurdering når en legger til barn`() {
        val (sak) = nySakMedVedtak(
            virkningsperiode = virkningsperiodeSøknadsbehandling,
        ).first.genererMeldeperioder(fixedClock)

        val førsteMeldeperiode = sak.meldeperiodeKjeder.first().siste

        val meldekortbehandling = meldekortBehandletAutomatisk(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            meldeperiode = førsteMeldeperiode,
        )

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            virkningsperiode = virkningsperiodeRevurdering,
        )

        val kommando = RevurderingInnvilgelseTilBeslutningKommando(
            sakId = sak.id,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler(),
            correlationId = CorrelationId.generate(),
            begrunnelse = BegrunnelseVilkårsvurdering("lol"),
            fritekstTilVedtaksbrev = null,
            innvilgelsesperiode = virkningsperiodeRevurdering,
            tiltaksdeltakelser = revurdering.saksopplysninger.tiltaksdeltagelse.map {
                Pair(virkningsperiodeRevurdering, it.eksternDeltagelseId)
            },
            barnetillegg = barnetillegg(
                periode = virkningsperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkningsperiodeRevurdering,
            ),
        )

        val sakMedRevurdering = sak.copy(
            behandlinger = sak.behandlinger.leggTilRevurdering(revurdering),
            meldekortBehandlinger = sak.meldekortBehandlinger.leggTil(meldekortbehandling),
        )

        val beregning = sakMedRevurdering.beregnRevurderingInnvilgelse(kommando).getOrFail()

        beregning.shouldBeInstanceOf<BehandlingBeregning>()
        beregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        beregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8

        beregning.totalBeløp shouldBe meldekortbehandling.beregning.ordinærBeløp + beregning.barnetilleggBeløp
    }

    @Test
    fun `Skal ikke beregne en revurdering dersom ingen tidligere beregninger`() {
        val (sak) = nySakMedVedtak(
            virkningsperiode = virkningsperiodeSøknadsbehandling,
        )

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
        )

        val kommando = RevurderingInnvilgelseTilBeslutningKommando(
            sakId = sak.id,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler(),
            correlationId = CorrelationId.generate(),
            begrunnelse = BegrunnelseVilkårsvurdering("lol"),
            fritekstTilVedtaksbrev = null,
            innvilgelsesperiode = virkningsperiodeRevurdering,
            tiltaksdeltakelser = revurdering.saksopplysninger.tiltaksdeltagelse.map {
                Pair(virkningsperiodeRevurdering, it.eksternDeltagelseId)
            },
            barnetillegg = null,
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkningsperiodeRevurdering,
            ),
        )

        val sakMedRevurdering = sak.copy(behandlinger = sak.behandlinger.leggTilRevurdering(revurdering))

        sakMedRevurdering.beregnRevurderingInnvilgelse(kommando).leftOrNull()
            .shouldBeInstanceOf<RevurderingIkkeBeregnet.IngenTidligereBeregninger>()
    }

    @Test
    fun `Skal ikke returnere beregning dersom det ikke er endringer i utbetaling`() {
        val (sak) = nySakMedVedtak(
            virkningsperiode = virkningsperiodeSøknadsbehandling,
        ).first.genererMeldeperioder(fixedClock)

        val førsteMeldeperiode = sak.meldeperiodeKjeder.first().siste

        val meldekortbehandling = meldekortBehandletAutomatisk(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            meldeperiode = førsteMeldeperiode,
        )

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            virkningsperiode = virkningsperiodeRevurdering,
        )

        val kommando = RevurderingInnvilgelseTilBeslutningKommando(
            sakId = sak.id,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler(),
            correlationId = CorrelationId.generate(),
            begrunnelse = BegrunnelseVilkårsvurdering("lol"),
            fritekstTilVedtaksbrev = null,
            innvilgelsesperiode = virkningsperiodeRevurdering,
            tiltaksdeltakelser = revurdering.saksopplysninger.tiltaksdeltagelse.map {
                Pair(virkningsperiodeRevurdering, it.eksternDeltagelseId)
            },
            barnetillegg = null,
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkningsperiodeRevurdering,
            ),
        )

        val sakMedRevurdering = sak.copy(
            behandlinger = sak.behandlinger.leggTilRevurdering(revurdering),
            meldekortBehandlinger = sak.meldekortBehandlinger.leggTil(meldekortbehandling),
        )

        sakMedRevurdering.beregnRevurderingInnvilgelse(kommando).leftOrNull()
            .shouldBeInstanceOf<RevurderingIkkeBeregnet.IngenEndring>()
    }
}
