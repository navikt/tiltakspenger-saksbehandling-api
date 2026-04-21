package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

enum class KanIkkeIverksetteUtbetalingDTO {
    SimuleringMangler,
    JusteringStøttesIkke,
    SimuleringHarEndringer,
    BehandlingstypeStøtterIkkeFeilutbetaling,
    BehandlingstypeStøtterIkkeJustering,
}

fun KanIkkeIverksetteUtbetaling.tilKanIkkeIverksetteUtbetalingDTO(): KanIkkeIverksetteUtbetalingDTO {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> KanIkkeIverksetteUtbetalingDTO.SimuleringMangler
        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> KanIkkeIverksetteUtbetalingDTO.JusteringStøttesIkke
        KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> KanIkkeIverksetteUtbetalingDTO.SimuleringHarEndringer
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> KanIkkeIverksetteUtbetalingDTO.BehandlingstypeStøtterIkkeFeilutbetaling
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering -> KanIkkeIverksetteUtbetalingDTO.BehandlingstypeStøtterIkkeJustering
    }
}
