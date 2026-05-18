package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import java.time.LocalDate

data class TilbakekrevingVenter(
    val grunn: TilbakekrevingVentegrunn,
    val gjenopptas: LocalDate,
) {

    enum class TilbakekrevingVentegrunn {
        AVVENTER_BRUKERUTTALELSE,
    }
}
