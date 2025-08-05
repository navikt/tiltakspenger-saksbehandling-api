package no.nav.tiltakspenger.saksbehandling.felles

data class SattPåVent(
    val erSattPåVent: Boolean = false,
    val sattPåVentBegrunnelser: List<SattPåVentBegrunnelse> = emptyList(),
)
