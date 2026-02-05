package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.toNonEmptySetOrThrow
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse.Companion.toBegrunnelse
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Innvilgelse::class, name = "REVURDERING_INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Stans::class, name = "STANS"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Omgjøring::class, name = "OMGJØRING"),
)
sealed interface OppdaterRevurderingDTO : OppdaterBehandlingDTO {

    data class Innvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
        val barnetillegg: BarnetilleggDTO,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterRevurderingKommando.Innvilgelse {
            val innvilgelsesperioder = innvilgelsesperioder.tilKommando()

            return OppdaterRevurderingKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.create(it) },
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperioder.perioder),
            )
        }
    }

    data class Omgjøring(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
        val barnetillegg: BarnetilleggDTO,
        val vedtaksperiode: PeriodeDTO,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterRevurderingKommando.Omgjøring {
            val innvilgelsesperioder = innvilgelsesperioder.tilKommando()

            return OppdaterRevurderingKommando.Omgjøring(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.create(it) },
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperioder.perioder),
                vedtaksperiode = vedtaksperiode.toDomain(),
            )
        }
    }

    data class Stans(
        override val begrunnelseVilkårsvurdering: String? = null,
        override val fritekstTilVedtaksbrev: String? = null,
        val valgteHjemler: List<ValgtHjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean,
        val stansFraOgMed: LocalDate?,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.STANS

        init {
            if (harValgtStansFraFørsteDagSomGirRett) {
                require(stansFraOgMed == null) { "stansFraOgMed må være null når harValgtStansFraFørsteDagSomGirRett er true" }
            } else {
                requireNotNull(stansFraOgMed) { "stansFraOgMed kan ikke være null når harValgtStansFraFørsteDagSomGirRett er false" }
            }
        }

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterRevurderingKommando.Stans {
            return Stans(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.create(it) },
                stansFraOgMed = ValgtStansFraOgMed.create(stansFraOgMed),
                valgteHjemler = valgteHjemler.toDomain().toNonEmptySetOrThrow(),
            )
        }
    }
}
