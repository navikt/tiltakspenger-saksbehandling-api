package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse.Companion.toBegrunnelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Innvilgelse::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Avslag::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.IkkeValgtResultat::class, name = "IKKE_VALGT"),
)
sealed interface OppdaterSøknadsbehandlingDTO : OppdaterBehandlingDTO {
    override fun tilDomene(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OppdaterSøknadsbehandlingKommando

    data class Innvilgelse(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val innvilgelsesperiode: PeriodeDTO,
        val barnetillegg: BarnetilleggDTO,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO>,
    ) : OppdaterSøknadsbehandlingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.INNVILGELSE

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterSøknadsbehandlingKommando.Innvilgelse {
            return OppdaterSøknadsbehandlingKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                innvilgelsesperioder = tilInnvilgelsesperioderKommando(
                    innvilgelsesperiode = innvilgelsesperiode,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiodeForPerioder,
                    tiltaksdeltakelser = valgteTiltaksdeltakelser,
                ),
                barnetillegg = barnetillegg.tilBarnetillegg(innvilgelsesperiode.toDomain()),
                automatiskSaksbehandlet = false,
            )
        }
    }

    data class Avslag(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>,
    ) : OppdaterSøknadsbehandlingDTO {
        override val resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.AVSLAG

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterSøknadsbehandlingKommando {
            return OppdaterSøknadsbehandlingKommando.Avslag(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
                avslagsgrunner = avslagsgrunner.toAvslagsgrunnlag(),
            )
        }
    }

    data class IkkeValgtResultat(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,

    ) : OppdaterSøknadsbehandlingDTO {
        override val resultat = null

        override fun tilDomene(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): OppdaterSøknadsbehandlingKommando {
            return OppdaterSøknadsbehandlingKommando.IkkeValgtResultat(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.toBegrunnelse(),
            )
        }
    }
}
