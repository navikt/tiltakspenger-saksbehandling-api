package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import java.time.LocalDateTime

data class VentestatusHendelseDbJson(
    val begrunnelse: String,
    val endretAv: String,
    val tidspunkt: String,
    val erSattPåVent: Boolean,
    val status: String,
)

fun VentestatusHendelseDbJson.toSattPåVentBegrunnelse(): VentestatusHendelse {
    return VentestatusHendelse(
        tidspunkt = LocalDateTime.parse(tidspunkt),
        endretAv = endretAv,
        begrunnelse = begrunnelse,
        erSattPåVent = erSattPåVent,
        status = status,
    )
}

fun VentestatusHendelse.toDbJson(): VentestatusHendelseDbJson = VentestatusHendelseDbJson(
    begrunnelse = this.begrunnelse,
    endretAv = this.endretAv,
    tidspunkt = this.tidspunkt.toString(),
    erSattPåVent = this.erSattPåVent,
    status = status,
)
