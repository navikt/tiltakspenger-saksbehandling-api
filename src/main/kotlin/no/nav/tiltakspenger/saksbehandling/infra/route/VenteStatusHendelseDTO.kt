package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse

data class VentestatusHendelseDTO(
    val sattPåVentAv: String,
    val tidspunkt: String,
    val begrunnelse: String,
    val erSattPåVent: Boolean,
    val status: BehandlingsstatusDTO,
)

fun VentestatusHendelse.tilVentestatusHendelseDTO() = VentestatusHendelseDTO(
    sattPåVentAv = endretAv,
    tidspunkt = tidspunkt.toString(),
    begrunnelse = begrunnelse,
    erSattPåVent = erSattPåVent,
    status = status.toBehandlingsstatusDTO(),
)
