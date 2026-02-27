package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

enum class KanIkkeIverksetteUtbetalingDTO {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
    SimuleringHarEndringer,
}

fun KanIkkeIverksetteUtbetaling.tilKanIkkeIverksetteUtbetalingDTO(): KanIkkeIverksetteUtbetalingDTO {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> KanIkkeIverksetteUtbetalingDTO.SimuleringMangler
        KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke -> KanIkkeIverksetteUtbetalingDTO.FeilutbetalingStøttesIkke
        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> KanIkkeIverksetteUtbetalingDTO.JusteringStøttesIkke
        KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> KanIkkeIverksetteUtbetalingDTO.SimuleringHarEndringer
    }
}
