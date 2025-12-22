package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse

interface OppdaterBehandlingKommandoMother : MotherOfAllMothers {

    fun innvilgelsesperiodeKommando(
        innvilgelsesperiode: Periode,
        antallDagerPerMeldeperiode: Int = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        tiltaksdeltakelseId: String = tiltaksdeltakelse().eksternDeltakelseId,
    ): InnvilgelsesperiodeKommando {
        return InnvilgelsesperiodeKommando(
            periode = innvilgelsesperiode,
            antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(antallDagerPerMeldeperiode),
            tiltaksdeltakelseId = tiltaksdeltakelseId,
        )
    }

    fun oppdaterSøknadsbehandlingInnvilgelseKommando(
        sakId: SakId = SakId.random(),
        behandlingId: BehandlingId = BehandlingId.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        fritekstTilVedtaksbrev: String? = null,
        begrunnelseVilkårsvurdering: String? = null,
        automatiskSaksbehandlet: Boolean = false,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando>,
        barnetillegg: Barnetillegg = barnetillegg(
            periode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
        ),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): OppdaterSøknadsbehandlingKommando.Innvilgelse {
        return OppdaterSøknadsbehandlingKommando.Innvilgelse(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.createOrThrow(it) },
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { Begrunnelse.createOrThrow(it) },
            automatiskSaksbehandlet = automatiskSaksbehandlet,
            innvilgelsesperioder = innvilgelsesperioder.tilPeriodisering(),
            barnetillegg = barnetillegg,
        )
    }

    fun oppdaterSøknadsbehandlingAvslagKommando(
        sakId: SakId = SakId.random(),
        behandlingId: BehandlingId = BehandlingId.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        fritekstTilVedtaksbrev: String? = null,
        begrunnelseVilkårsvurdering: String? = null,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag> = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): OppdaterSøknadsbehandlingKommando.Avslag {
        return OppdaterSøknadsbehandlingKommando.Avslag(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.createOrThrow(it) },
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { Begrunnelse.createOrThrow(it) },
            avslagsgrunner = avslagsgrunner,
        )
    }

    fun oppdaterRevurderingInnvilgelseKommando(
        sakId: SakId = SakId.random(),
        behandlingId: BehandlingId = BehandlingId.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        fritekstTilVedtaksbrev: String? = null,
        begrunnelseVilkårsvurdering: String? = null,
        automatiskSaksbehandlet: Boolean = false,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando>,
        barnetillegg: Barnetillegg = barnetillegg(
            periode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
        ),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): OppdaterRevurderingKommando.Innvilgelse {
        return OppdaterRevurderingKommando.Innvilgelse(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.createOrThrow(it) },
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { Begrunnelse.createOrThrow(it) },
            innvilgelsesperioder = innvilgelsesperioder.tilPeriodisering(),
            barnetillegg = barnetillegg,
        )
    }
}

fun List<InnvilgelsesperiodeKommando>.tilPeriodisering(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    return this.map {
        PeriodeMedVerdi(
            verdi = it,
            periode = it.periode,
        )
    }.tilIkkeTomPeriodisering()
}
