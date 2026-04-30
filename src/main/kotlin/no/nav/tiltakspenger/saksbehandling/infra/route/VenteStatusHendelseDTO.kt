package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse

data class VentestatusHendelseDTO(
    val sattPåVentAv: String,
    val tidspunkt: String,
    val status: String,
    val begrunnelse: String,
    val erSattPåVent: Boolean,
    val frist: String? = null,
)

fun List<VentestatusHendelse>.tilDto(): List<VentestatusHendelseDTO> = this.map { it.tilVentestatusHendelseDTO() }

fun VentestatusHendelse.tilVentestatusHendelseDTO() = VentestatusHendelseDTO(
    sattPåVentAv = endretAv,
    tidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
    erSattPåVent = erSattPåVent,
    frist = frist?.toString(),
    status = this.status,
)
