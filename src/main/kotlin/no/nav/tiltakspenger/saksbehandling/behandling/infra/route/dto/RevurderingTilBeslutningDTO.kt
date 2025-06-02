package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingInnvilgelseTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingStansTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import java.time.LocalDate

data class RevurderingTilBeslutningDTO(
    val type: RevurderingTypeDTO,
    val begrunnelse: String,
    val fritekstTilVedtaksbrev: String?,
    val stans: Stans?,
    val innvilgelse: Innvilgelse?,
) {

    data class Stans(
        val valgteHjemler: List<ValgtHjemmelForStansDTO>,
        val stansFraOgMed: LocalDate,
    )

    data class Innvilgelse(
        val innvilgelsesperiode: PeriodeDTO,
        val forrigeBehandlingId: String,
    )

    fun tilKommando(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): RevurderingTilBeslutningKommando {
        return when (type) {
            RevurderingTypeDTO.STANS -> {
                requireNotNull(stans)

                RevurderingStansTilBeslutningKommando(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                    begrunnelse = BegrunnelseVilkårsvurdering(saniter(begrunnelse)),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                    stansFraOgMed = stans.stansFraOgMed,
                    valgteHjemler = stans.valgteHjemler.toDomain(),
                    sisteDagSomGirRett = null,
                )
            }

            RevurderingTypeDTO.INNVILGELSE -> {
                requireNotNull(innvilgelse)

                RevurderingInnvilgelseTilBeslutningKommando(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                    begrunnelse = BegrunnelseVilkårsvurdering(saniter(begrunnelse)),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                    innvilgelsesperiode = innvilgelse.innvilgelsesperiode.toDomain(),
                )
            }
        }
    }
}

enum class RevurderingTypeDTO {
    STANS,
    INNVILGELSE,
    ;

    fun tilRevurderingType(): RevurderingType = when (this) {
        STANS -> RevurderingType.STANS
        INNVILGELSE -> RevurderingType.INNVILGELSE
    }
}
