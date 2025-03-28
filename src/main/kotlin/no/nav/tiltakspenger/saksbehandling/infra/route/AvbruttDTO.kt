package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt

internal data class AvbruttDTO(
    val avbruttAv: String,
    val avbruttTidspunkt: String,
    val begrunnelse: String,
)

internal fun no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt.toAvbruttDTO() = AvbruttDTO(
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
)
