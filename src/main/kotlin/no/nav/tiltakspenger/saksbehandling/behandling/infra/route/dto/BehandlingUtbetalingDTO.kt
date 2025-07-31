package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val beregninger: List<MeldeperiodeBeregningDTO>,
)

fun Utbetaling.tilDTO(): BehandlingUtbetalingDTO {
    return BehandlingUtbetalingDTO(
        navkontor = this.navkontor.kontornummer,
        navkontorNavn = this.navkontor.kontornavn,
        beregninger = this.beregning.beregninger.map { it.tilMeldeperiodeBeregningDTO() },
    )
}
