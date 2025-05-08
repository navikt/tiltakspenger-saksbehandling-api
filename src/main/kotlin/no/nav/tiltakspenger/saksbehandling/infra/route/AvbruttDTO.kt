package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt

data class AvbruttDTO(
    val avbruttAv: String,
    val avbruttTidspunkt: String,
    val begrunnelse: String,
)

fun Avbrutt.toAvbruttDTO() = AvbruttDTO(
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
