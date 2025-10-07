package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.beregning.beregnBarnetilleggBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOrdinærBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.beregnTotalBeløp
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val status: UtbetalingsstatusDTO,
    val beregninger: List<MeldeperiodeBeregningDTO>,
    val beregningerSummert: BeregningerSummertDTO,
    val simulertBeregning: SimulertBeregningDTO,
)

data class BeregningerSummertDTO(
    val totalt: BeløpFørOgNåDTO,
    val ordinært: BeløpFørOgNåDTO,
    val barnetillegg: BeløpFørOgNåDTO,
)

data class BeløpFørOgNåDTO(
    val før: Int?,
    val nå: Int,
)

fun BehandlingUtbetaling.tilDTO(
    meldeperiodeBeregninger: MeldeperiodeBeregninger,
    utbetalingsstatus: Utbetalingsstatus?,
    tidligereUtbetalinger: Utbetalinger,
): BehandlingUtbetalingDTO {
    val forrigeBeregninger: List<MeldeperiodeBeregning> =
        beregning.beregninger.map { meldeperiodeBeregninger.sisteBeregningFør(it.id, it.kjedeId)!! }

    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingsstatus.toUtbetalingsstatusDTO(),
        beregninger = beregning.beregninger.map { it.tilMeldeperiodeBeregningDTO() },
        beregningerSummert = BeregningerSummertDTO(
            totalt = BeløpFørOgNåDTO(
                før = forrigeBeregninger.beregnTotalBeløp(),
                nå = beregning.totalBeløp,
            ),
            ordinært = BeløpFørOgNåDTO(
                før = forrigeBeregninger.beregnOrdinærBeløp(),
                nå = beregning.ordinærBeløp,
            ),
            barnetillegg = BeløpFørOgNåDTO(
                før = forrigeBeregninger.beregnBarnetilleggBeløp(),
                nå = beregning.barnetilleggBeløp,
            ),
        ),
        simulertBeregning = this.toSimulertBeregning(tidligereUtbetalinger).toSimulertBeregningDTO(),
    )
}
