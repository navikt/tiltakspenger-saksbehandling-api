package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.toNonEmptyListOrThrow
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansTilOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
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
        val innvilgelsesperioder: List<InnvilgelsesperiodeDTO>,
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
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperioder.totalPeriode),
            )
        }
    }

    data class Omgjøring(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperioder: List<InnvilgelsesperiodeDTO>,
        val barnetillegg: BarnetilleggDTO,
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
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperioder.totalPeriode),
            )
        }
    }

    data class Stans(
        override val begrunnelseVilkårsvurdering: String? = null,
        override val fritekstTilVedtaksbrev: String? = null,
        val valgteHjemler: List<ValgtHjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean,
        val harValgtStansTilSisteDagSomGirRett: Boolean,
        val stansFraOgMed: LocalDate?,
        val stansTilOgMed: LocalDate?,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.STANS

        init {
            if (harValgtStansFraFørsteDagSomGirRett) require(stansFraOgMed == null) { "stansFraOgMed må være null når harValgtStansFraFørsteDagSomGirRett er true" }
            if (harValgtStansTilSisteDagSomGirRett) require(stansTilOgMed == null) { "stansTilOgMed må være null når harValgtStansTilSisteDagSomGirRett er true" }
            if (!harValgtStansFraFørsteDagSomGirRett) requireNotNull(stansFraOgMed) { "stansFraOgMed kan ikke være null når harValgtStansFraFørsteDagSomGirRett er false" }
            if (!harValgtStansTilSisteDagSomGirRett) requireNotNull(stansTilOgMed) { "stansTilOgMed kan ikke være null når harValgtStansTilSisteDagSomGirRett er false" }
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
                stansTilOgMed = ValgtStansTilOgMed.create(stansTilOgMed),
                valgteHjemler = valgteHjemler.toDomain().toNonEmptyListOrThrow(),
            )
        }
    }
}

fun RammebehandlingResultatTypeDTO.tilRevurderingType(): RevurderingType = when (this) {
    RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
    RammebehandlingResultatTypeDTO.STANS -> RevurderingType.STANS
    RammebehandlingResultatTypeDTO.OMGJØRING -> RevurderingType.OMGJØRING

    RammebehandlingResultatTypeDTO.AVSLAG,
    RammebehandlingResultatTypeDTO.INNVILGELSE,
    RammebehandlingResultatTypeDTO.IKKE_VALGT,
    -> throw IllegalStateException("Ugyldig type for revurdering $this")
}

fun RevurderingType.tilDTO(): RammebehandlingResultatTypeDTO = when (this) {
    RevurderingType.STANS -> RammebehandlingResultatTypeDTO.STANS
    RevurderingType.INNVILGELSE -> RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE
    RevurderingType.OMGJØRING -> RammebehandlingResultatTypeDTO.OMGJØRING
}
