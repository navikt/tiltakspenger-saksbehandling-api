package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingInnvilgelseTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingStansTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import java.time.LocalDate
import kotlin.collections.List

data class RevurderingTilBeslutningDTO(
    val type: BehandlingResultatDTO,
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
        val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val barnetillegg: BarnetilleggDTO?,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO> = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    )

    fun tilKommando(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): RevurderingTilBeslutningKommando {
        return when (type) {
            BehandlingResultatDTO.STANS -> {
                requireNotNull(stans)

                RevurderingStansTilBeslutningKommando(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                    begrunnelse = BegrunnelseVilkårsvurdering(saniter(begrunnelse)),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                    stansFraOgMed = stans.stansFraOgMed,
                    valgteHjemler = stans.valgteHjemler.toDomain().toNonEmptyListOrThrow(),
                    sisteDagSomGirRett = null,
                )
            }

            BehandlingResultatDTO.REVURDERING_INNVILGELSE -> {
                requireNotNull(innvilgelse)

                val innvilgelsesperiode = innvilgelse.innvilgelsesperiode.toDomain()

                RevurderingInnvilgelseTilBeslutningKommando(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                    begrunnelse = BegrunnelseVilkårsvurdering(saniter(begrunnelse)),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                    innvilgelsesperiode = innvilgelsesperiode,
                    tiltaksdeltakelser = innvilgelse.valgteTiltaksdeltakelser.map {
                        Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                    },
                    barnetillegg = innvilgelse.barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
                    antallDagerPerMeldeperiode = Periodisering(
                        innvilgelse.antallDagerPerMeldeperiodeForPerioder.map {
                            PeriodeMedVerdi(AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode), it.periode.toDomain())
                        },
                    ),
                )
            }

            BehandlingResultatDTO.INNVILGELSE,
            BehandlingResultatDTO.AVSLAG,
            -> throw IllegalStateException("Ugyldig type for revurdering $this")
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
