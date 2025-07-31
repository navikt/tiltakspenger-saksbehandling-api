package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Innvilgelse::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Avslag::class, name = "AVSLAG"),
)
sealed interface OppdaterSøknadsbehandlingDTO : OppdaterBehandlingDTO {
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>

    override fun tilDomene(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): SendSøknadsbehandlingTilBeslutningKommando

    data class Innvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val innvilgelsesperiode: PeriodeDTO,
        val barnetillegg: BarnetilleggDTO?,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO>? = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    ) : OppdaterSøknadsbehandlingDTO {

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse {
            val innvilgelsesperiode = innvilgelsesperiode.toDomain()

            return SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
                innvilgelsesperiode = innvilgelsesperiode,
                barnetillegg = barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                antallDagerPerMeldeperiode =
                antallDagerPerMeldeperiodeForPerioder?.map {
                    PeriodeMedVerdi(
                        AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                        it.periode.toDomain(),
                    )
                }?.tilSammenhengendePeriodisering(),
            )
        }
    }

    data class Avslag(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>,
    ) : OppdaterSøknadsbehandlingDTO {

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): SendSøknadsbehandlingTilBeslutningKommando {
            return SendSøknadsbehandlingTilBeslutningKommando.Avslag(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                avslagsgrunner = avslagsgrunner.toAvslagsgrunnlag(),
            )
        }
    }
}
