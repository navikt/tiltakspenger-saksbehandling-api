package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.SattPåVentBegrunnelse

data class SattPåVentBegrunnelseDTO(
    val saksbehandler: String,
    val tidspunkt: String,
    val begrunnelse: String,
)

fun SattPåVentBegrunnelse.tilSattPåVentBegrunnelseDTO() = SattPåVentBegrunnelseDTO(
    saksbehandler = saksbehandler,
    tidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
