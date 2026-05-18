package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil

enum class TilbakekrevinghendelseFeilDb {
    FantIkkeSak,
    FantIkkeBehandling,
    FantIkkeUtbetaling,
    ;

    fun tilDomene(): TilbakekrevinghendelseFeil = when (this) {
        FantIkkeSak -> TilbakekrevinghendelseFeil.FantIkkeSak
        FantIkkeBehandling -> TilbakekrevinghendelseFeil.FantIkkeBehandling
        FantIkkeUtbetaling -> TilbakekrevinghendelseFeil.FantIkkeUtbetaling
    }
}

fun TilbakekrevinghendelseFeil.tilDb(): String = when (this) {
    TilbakekrevinghendelseFeil.FantIkkeSak -> TilbakekrevinghendelseFeilDb.FantIkkeSak
    TilbakekrevinghendelseFeil.FantIkkeBehandling -> TilbakekrevinghendelseFeilDb.FantIkkeBehandling
    TilbakekrevinghendelseFeil.FantIkkeUtbetaling -> TilbakekrevinghendelseFeilDb.FantIkkeUtbetaling
}.name
