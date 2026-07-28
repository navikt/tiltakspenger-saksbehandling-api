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
        is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> KanIkkeIverksetteUtbetalingDTO.JusteringStøttesIkke
        is KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> KanIkkeIverksetteUtbetalingDTO.SimuleringHarEndringer
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> KanIkkeIverksetteUtbetalingDTO.BehandlingstypeStøtterIkkeFeilutbetaling
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering -> KanIkkeIverksetteUtbetalingDTO.BehandlingstypeStøtterIkkeJustering
    }
}

/**
 * Melding som kan vises direkte til saksbehandler, for utfallene der domenet har mer å fortelle enn grunnen alene.
 * Frontenden faller tilbake til sin egen tekst per grunn når denne er null.
 */
fun KanIkkeIverksetteUtbetaling.tilMeldingDTO(): String? {
    return when (this) {
        is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> beskrivelse
        else -> null
    }
}
