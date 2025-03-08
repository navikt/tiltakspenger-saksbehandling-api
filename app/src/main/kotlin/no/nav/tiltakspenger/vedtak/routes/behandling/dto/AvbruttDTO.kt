package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.domene.behandling.Avbrutt

internal data class AvbruttDTO(
    val avbruttAv: String,
    val avbruttTidspunkt: String,
    val begrunnelse: String,
)

internal fun Avbrutt.toDTO() = AvbruttDTO(
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
