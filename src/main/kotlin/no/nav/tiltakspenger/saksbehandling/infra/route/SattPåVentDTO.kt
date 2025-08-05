package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.SattPåVent

data class SattPåVentDTO(
    val erSattPåVent: Boolean,
    val sattPåVentBegrunnelse: SattPåVentBegrunnelseDTO?,
)

fun SattPåVent.tilSattPåVentDTO() = SattPåVentDTO(
    erSattPåVent = erSattPåVent,
    sattPåVentBegrunnelse = sattPåVentBegrunnelser.lastOrNull()?.tilSattPåVentBegrunnelseDTO(),
)
