package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando.Innvilgelse.InnvilgelsesperiodeKommando

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Innvilgelse::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.Avslag::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = OppdaterSøknadsbehandlingDTO.IkkeValgtResultat::class, name = "IKKE_VALGT"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Innvilgelse::class, name = "REVURDERING_INNVILGELSE"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Stans::class, name = "STANS"),
    JsonSubTypes.Type(value = OppdaterRevurderingDTO.Omgjøring::class, name = "OMGJØRING"),
)
sealed interface OppdaterBehandlingDTO {
    val resultat: RammebehandlingResultatTypeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?

    fun tilDomene(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OppdaterBehandlingKommando

    fun List<InnvilgelsesperiodeDTO>.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
        return this.map {
            val periode = it.periode.toDomain()

            PeriodeMedVerdi(
                periode = periode,
                verdi = InnvilgelsesperiodeKommando(
                    periode = periode,
                    antallDagerPerMeldeperiode = it.antallDagerPerMeldeperiode,
                    tiltaksdeltakelseId = it.tiltaksdeltakelseId,
                ),
            )
        }.tilIkkeTomPeriodisering()
    }
}
