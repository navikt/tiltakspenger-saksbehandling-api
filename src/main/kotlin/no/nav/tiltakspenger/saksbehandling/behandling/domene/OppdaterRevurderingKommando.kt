package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
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
        val valgteHjemler: NonEmptyList<ValgtHjemmelForStans>,
        val stansFraOgMed: ValgtStansFraOgMed,
        val stansTilOgMed: ValgtStansTilOgMed,
    ) : OppdaterRevurderingKommando {

        /** Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett */
        val harValgtStansFraFørsteDagSomGirRett: Boolean get() = stansFraOgMed is ValgtStansFraOgMed.StansFraFørsteDagSomGirRett

        /** Dersom saksbehandler har valgt at det skal stanses til siste dag som gir rett */
        val harValgtStansTilSisteDagSomGirRett: Boolean get() = stansTilOgMed is ValgtStansTilOgMed.StansTilSisteDagSomGirRett

        fun utledStansperiode(førsteDagSomGirRett: LocalDate, sisteDagSomGirRett: LocalDate): Periode {
            if (stansFraOgMed.stansFraOgMed != null) {
                require(stansFraOgMed.stansFraOgMed!! >= førsteDagSomGirRett) {
                    "Stans fra og med ${stansFraOgMed.stansFraOgMed} kan ikke være før første dag som gir rett $førsteDagSomGirRett"
                }
            }
            if (stansTilOgMed.stansTilOgMed != null) {
                require(stansTilOgMed.stansTilOgMed!! <= sisteDagSomGirRett) {
                    "Stans til og med ${stansTilOgMed.stansTilOgMed} kan ikke være etter siste dag som gir rett $sisteDagSomGirRett"
                }
            }
            return Periode(
                fraOgMed = hentStansFraOgMed(førsteDagSomGirRett),
                tilOgMed = hentStansTilOgMed(sisteDagSomGirRett),
            )
        }

        private fun hentStansFraOgMed(førsteDagSomGirRett: LocalDate): LocalDate {
            return stansFraOgMed.stansFraOgMed ?: førsteDagSomGirRett
        }

        private fun hentStansTilOgMed(sisteDagSomGirRett: LocalDate): LocalDate {
            return stansTilOgMed.stansTilOgMed ?: sisteDagSomGirRett
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

        sealed interface ValgtStansTilOgMed {
            val stansTilOgMed: LocalDate?

            data object StansTilSisteDagSomGirRett : ValgtStansTilOgMed {
                override val stansTilOgMed = null
            }

            data class StansTilOgMed(override val stansTilOgMed: LocalDate) : ValgtStansTilOgMed

            companion object {
                fun create(dato: LocalDate?): ValgtStansTilOgMed {
                    return if (dato == null) {
                        StansTilSisteDagSomGirRett
                    } else {
                        StansTilOgMed(dato)
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
        override val innvilgelsesperiode: Periode,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
    ) : OppdaterRevurderingKommando,
        OppdaterBehandlingKommando.Innvilgelse

    data class Omgjøring(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val innvilgelsesperiode: Periode,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
    ) : OppdaterRevurderingKommando,
        OppdaterBehandlingKommando.Innvilgelse {

        fun utledNyVirkningsperiode(
            eksisterendeVirkningsperiode: Periode,
            nyInnvilgelsesperiode: Periode,
        ) = RevurderingResultat.Omgjøring.utledNyVirkningsperiode(
            eksisterendeVirkningsperiode = eksisterendeVirkningsperiode,
            nyInnvilgelsesperiode = nyInnvilgelsesperiode,
        )
    }
}
