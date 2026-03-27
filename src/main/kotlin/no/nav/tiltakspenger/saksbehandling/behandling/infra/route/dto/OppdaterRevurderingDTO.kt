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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse.Companion.toBegrunnelse
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Innvilgelse::class, name = "REVURDERING_INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Stans::class, name = "STANS"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.OmgjøringInnvilgelse::class, name = "OMGJØRING"),
)
sealed interface OppdaterRevurderingDTO : OppdaterBehandlingDTO {

    data class Innvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
        val barnetillegg: BarnetilleggDTO,
        override val skalSendeVedtaksbrev: Boolean = true,
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
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            )
        }
    }

    data class Stans(
        override val begrunnelseVilkårsvurdering: String? = null,
        override val fritekstTilVedtaksbrev: String? = null,
        val valgteHjemler: List<HjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean,
        val stansFraOgMed: LocalDate?,
        override val skalSendeVedtaksbrev: Boolean = true,
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
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            )
        }
    }

    data class OmgjøringInnvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperioder: InnvilgelsesperioderDTO,
        val barnetillegg: BarnetilleggDTO,
        val vedtaksperiode: PeriodeDTO,
        override val skalSendeVedtaksbrev: Boolean = true,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.OMGJØRING

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterOmgjøringKommando.OmgjøringInnvilgelse {
            val innvilgelsesperioder = innvilgelsesperioder.tilKommando()

            return OppdaterOmgjøringKommando.OmgjøringInnvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.create(it) },
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperioder.perioder),
                vedtaksperiode = vedtaksperiode.toDomain(),
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            )
        }
    }

    data class OmgjøringOpphør(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val vedtaksperiode: PeriodeDTO,
        val valgteHjemler: List<HjemmelForOpphørDTO>,
        override val skalSendeVedtaksbrev: Boolean = true,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.OMGJØRING_OPPHØR

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterOmgjøringKommando.OmgjøringOpphør {
            return OppdaterOmgjøringKommando.OmgjøringOpphør(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.create(it) },
                vedtaksperiode = vedtaksperiode.toDomain(),
                valgteHjemler = valgteHjemler.toDomain(),
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            )
        }
    }

    data object OmgjøringIkkeValgt : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT
        override val fritekstTilVedtaksbrev = null
        override val begrunnelseVilkårsvurdering = null
        override val skalSendeVedtaksbrev = true

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterOmgjøringKommando.OmgjøringIkkeValgt {
            return OppdaterOmgjøringKommando.OmgjøringIkkeValgt(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
            )
        }
    }
}
