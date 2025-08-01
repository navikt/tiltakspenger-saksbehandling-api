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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
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
        val barnetillegg: BarnetilleggDTO?,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO> = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    ) : OppdaterRevurderingDTO {
        override val resultat: BehandlingResultatDTO = BehandlingResultatDTO.REVURDERING_INNVILGELSE

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
                barnetillegg = barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
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
        val stansFraOgMed: LocalDate,
    ) : OppdaterRevurderingDTO {
        override val resultat: BehandlingResultatDTO = BehandlingResultatDTO.STANS

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
                stansFraOgMed = stansFraOgMed,
                valgteHjemler = valgteHjemler.tilBeslutningKommando().toNonEmptyListOrThrow(),
                sisteDagSomGirRett = null,
            )
        }
    }
}

fun BehandlingResultatDTO.tilRevurderingType(): RevurderingType = when (this) {
    BehandlingResultatDTO.REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
    BehandlingResultatDTO.STANS -> RevurderingType.STANS
    BehandlingResultatDTO.AVSLAG,
    BehandlingResultatDTO.INNVILGELSE,
    -> throw IllegalStateException("Ugyldig type for revurdering $this")
}

fun RevurderingType.tilDTO(): BehandlingResultatDTO = when (this) {
    RevurderingType.STANS -> BehandlingResultatDTO.STANS
    RevurderingType.INNVILGELSE -> BehandlingResultatDTO.REVURDERING_INNVILGELSE
}
