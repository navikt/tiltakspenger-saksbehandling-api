package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import java.time.LocalDate

sealed interface OppdaterRevurderingKommando : OppdaterBehandlingKommando {
    override val sakId: SakId
    override val behandlingId: BehandlingId
    override val saksbehandler: Saksbehandler
    override val correlationId: CorrelationId
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    override val begrunnelseVilkårsvurdering: Begrunnelse?

    data class Stans(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        val valgteHjemler: NonEmptySet<ValgtHjemmelForStans>,
        val stansFraOgMed: ValgtStansFraOgMed,
    ) : OppdaterRevurderingKommando {

        /** Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett */
        val harValgtStansFraFørsteDagSomGirRett: Boolean get() = stansFraOgMed is ValgtStansFraOgMed.StansFraFørsteDagSomGirRett

        fun utledStansperiode(førsteDagSomGirRett: LocalDate, sisteDagSomGirRett: LocalDate): Periode {
            if (stansFraOgMed.stansFraOgMed != null) {
                require(stansFraOgMed.stansFraOgMed!! >= førsteDagSomGirRett) {
                    "Stans fra og med ${stansFraOgMed.stansFraOgMed} kan ikke være før første dag som gir rett $førsteDagSomGirRett"
                }
            }
            return Periode(
                fraOgMed = hentStansFraOgMed(førsteDagSomGirRett),
                tilOgMed = sisteDagSomGirRett,
            )
        }

        private fun hentStansFraOgMed(førsteDagSomGirRett: LocalDate): LocalDate {
            return stansFraOgMed.stansFraOgMed ?: førsteDagSomGirRett
        }

        sealed interface ValgtStansFraOgMed {
            val stansFraOgMed: LocalDate?

            data object StansFraFørsteDagSomGirRett : ValgtStansFraOgMed {
                override val stansFraOgMed = null
            }

            data class StansFraOgMed(override val stansFraOgMed: LocalDate) : ValgtStansFraOgMed

            companion object {
                fun create(dato: LocalDate?): ValgtStansFraOgMed {
                    return if (dato == null) {
                        StansFraFørsteDagSomGirRett
                    } else {
                        StansFraOgMed(dato)
                    }
                }
            }
        }
    }

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
        override val barnetillegg: Barnetillegg,
    ) : OppdaterRevurderingKommando,
        OppdaterBehandlingKommando.Innvilgelse

    /**
     *  [vedtaksperiode] Hvis null skal hele den gjeldende vedtaksperioden omgjøres
     * */
    data class Omgjøring(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
        override val barnetillegg: Barnetillegg,
        val vedtaksperiode: Periode?,
    ) : OppdaterRevurderingKommando,
        OppdaterBehandlingKommando.Innvilgelse
}
