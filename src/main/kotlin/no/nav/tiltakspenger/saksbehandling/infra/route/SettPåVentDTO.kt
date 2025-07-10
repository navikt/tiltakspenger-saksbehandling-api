package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.SattPåVentBegrunnelse

data class SattPåVentBegrunnelseDTO(
    val sattPåVentAv: String,
    val tidspunkt: String,
    val begrunnelse: String,
)

fun SattPåVentBegrunnelse.tilSattPåVentBegrunnelseDTO() = SattPåVentBegrunnelseDTO(
    sattPåVentAv = sattPåVentAv,
    tidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
