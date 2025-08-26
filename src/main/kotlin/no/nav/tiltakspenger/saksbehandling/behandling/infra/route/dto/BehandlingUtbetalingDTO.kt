package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.beregning.beregnBarnetilleggBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOrdinærBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.beregnTotalBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val status: UtbetalingsstatusDTO,
    val beregninger: List<MeldeperiodeBeregningDTO>,
    val beregningerSummert: BeregningerSummertDTO,
)

data class BeregningerSummertDTO(
    val totalt: BeløpFørOgNå,
    val ordinært: BeløpFørOgNå,
    val barnetillegg: BeløpFørOgNå,
)

data class BeløpFørOgNå(
    val før: Int,
    val nå: Int,
)

fun BehandlingUtbetaling.tilDTO(
    meldeperiodeBeregninger: MeldeperiodeBeregninger,
    utbetalingFraVedtak: VedtattUtbetaling?,
): BehandlingUtbetalingDTO {
    val forrigeBeregninger: List<MeldeperiodeBeregning> =
        beregning.beregninger.map { meldeperiodeBeregninger.sisteBeregningFør(it.id, it.kjedeId)!! }

    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingFraVedtak?.status.toUtbetalingsstatusDTO(),
        beregninger = beregning.beregninger.map { it.tilMeldeperiodeBeregningDTO() },
        beregningerSummert = BeregningerSummertDTO(
            totalt = BeløpFørOgNå(
                før = forrigeBeregninger.beregnTotalBeløp(),
                nå = beregning.totalBeløp,
            ),
            ordinært = BeløpFørOgNå(
                før = forrigeBeregninger.beregnOrdinærBeløp(),
                nå = beregning.ordinærBeløp,
            ),
            barnetillegg = BeløpFørOgNå(
                før = forrigeBeregninger.beregnBarnetilleggBeløp(),
                nå = beregning.barnetilleggBeløp,
            ),
        ),
    )
}
