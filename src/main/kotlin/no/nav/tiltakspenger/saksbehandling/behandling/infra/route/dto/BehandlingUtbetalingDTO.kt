package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val beregninger: List<MeldeperiodeBeregningDTO>,
    val totalBeløp: BeløpDTO,
)

fun Utbetaling.tilDTO(): BehandlingUtbetalingDTO {
    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        beregninger = beregning.beregninger.map { it.tilMeldeperiodeBeregningDTO() },
        totalBeløp = BeløpDTO(
            totalt = beregning.totalBeløp,
            ordinært = beregning.ordinærBeløp,
            barnetillegg = beregning.barnetilleggBeløp,
        ),
    )
}
