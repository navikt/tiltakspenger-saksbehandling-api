package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

enum class KanIkkeIverksetteUtbetalingDTO {
    SimuleringMangler,
    JusteringStøttesIkke,
    SimuleringHarEndringer,
}

fun KanIkkeIverksetteUtbetaling.tilKanIkkeIverksetteUtbetalingDTO(): KanIkkeIverksetteUtbetalingDTO {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> KanIkkeIverksetteUtbetalingDTO.SimuleringMangler
        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> KanIkkeIverksetteUtbetalingDTO.JusteringStøttesIkke
        KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> KanIkkeIverksetteUtbetalingDTO.SimuleringHarEndringer
    }
}
