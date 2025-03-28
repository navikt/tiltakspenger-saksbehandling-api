package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Avbrutt

internal data class AvbruttDTO(
    val avbruttAv: String,
    val avbruttTidspunkt: String,
    val begrunnelse: String,
)

internal fun Avbrutt.toAvbruttDTO() = AvbruttDTO(
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
