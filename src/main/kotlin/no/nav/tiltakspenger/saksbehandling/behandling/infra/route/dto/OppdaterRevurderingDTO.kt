package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.toNonEmptyListOrThrow
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans.ValgtStansTilOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import java.time.LocalDate
import kotlin.collections.List

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Innvilgelse::class, name = "REVURDERING_INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Stans::class, name = "STANS"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Omgjøring::class, name = "OMGJØRING"),
)
sealed interface OppdaterRevurderingDTO : OppdaterBehandlingDTO {

    override fun tilDomene(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OppdaterRevurderingKommando

    data class Innvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperiode: PeriodeDTO,
        val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val barnetillegg: BarnetilleggDTO,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO> = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatDTO = RammebehandlingResultatDTO.REVURDERING_INNVILGELSE

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterRevurderingKommando.Innvilgelse {
            val innvilgelsesperiode = innvilgelsesperiode.toDomain()

            return OppdaterRevurderingKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering(saniter(begrunnelseVilkårsvurdering ?: "")),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                innvilgelsesperiode = innvilgelsesperiode,
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperiode),
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiodeForPerioder.map {
                    PeriodeMedVerdi(
                        AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                        it.periode.toDomain(),
                    )
                }.tilSammenhengendePeriodisering(),
            )
        }
    }

    data class Omgjøring(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val innvilgelsesperiode: PeriodeDTO,
        val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val barnetillegg: BarnetilleggDTO,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO> = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatDTO = RammebehandlingResultatDTO.REVURDERING_INNVILGELSE

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterRevurderingKommando.Omgjøring {
            val innvilgelsesperiode = innvilgelsesperiode.toDomain()

            return OppdaterRevurderingKommando.Omgjøring(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering(saniter(begrunnelseVilkårsvurdering ?: "")),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                innvilgelsesperiode = innvilgelsesperiode,
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperiode),
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiodeForPerioder.map {
                    PeriodeMedVerdi(
                        AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                        it.periode.toDomain(),
                    )
                }.tilSammenhengendePeriodisering(),
            )
        }
    }

    data class Stans(
        override val begrunnelseVilkårsvurdering: String?,
        override val fritekstTilVedtaksbrev: String?,
        val valgteHjemler: List<ValgtHjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean,
        val harValgtStansTilSisteDagSomGirRett: Boolean,
        val stansFraOgMed: LocalDate?,
        val stansTilOgMed: LocalDate?,
    ) : OppdaterRevurderingDTO {
        override val resultat: RammebehandlingResultatDTO = RammebehandlingResultatDTO.STANS

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
                begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering(saniter(begrunnelseVilkårsvurdering ?: "")),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                stansFraOgMed = ValgtStansFraOgMed.create(stansFraOgMed),
                stansTilOgMed = ValgtStansTilOgMed.create(stansTilOgMed),
                valgteHjemler = valgteHjemler.toDomain().toNonEmptyListOrThrow(),
            )
        }
    }
}

fun RammebehandlingResultatDTO.tilRevurderingType(): RevurderingType = when (this) {
    RammebehandlingResultatDTO.REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
    RammebehandlingResultatDTO.STANS -> RevurderingType.STANS
    RammebehandlingResultatDTO.OMGJØRING -> RevurderingType.OMGJØRING

    RammebehandlingResultatDTO.AVSLAG,
    RammebehandlingResultatDTO.INNVILGELSE,
    RammebehandlingResultatDTO.IKKE_VALGT,
    -> throw IllegalStateException("Ugyldig type for revurdering $this")
}

fun RevurderingType.tilDTO(): RammebehandlingResultatDTO = when (this) {
    RevurderingType.STANS -> RammebehandlingResultatDTO.STANS
    RevurderingType.INNVILGELSE -> RammebehandlingResultatDTO.REVURDERING_INNVILGELSE
    RevurderingType.OMGJØRING -> RammebehandlingResultatDTO.OMGJØRING
}
