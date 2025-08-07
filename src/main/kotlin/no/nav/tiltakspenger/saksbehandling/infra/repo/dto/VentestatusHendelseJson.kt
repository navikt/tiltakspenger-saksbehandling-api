package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import java.time.LocalDateTime

data class VentestatusHendelseJson(
    val begrunnelse: String,
    val endretAv: String,
    val tidspunkt: String,
    val erSattPåVent: Boolean,
    val status: Behandlingsstatus,
) {
    enum class Behandlingsstatus {
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
    }
}

fun VentestatusHendelseJson.toSattPåVentBegrunnelse(): VentestatusHendelse {
    return VentestatusHendelse(
        tidspunkt = LocalDateTime.parse(tidspunkt),
        endretAv = endretAv,
        begrunnelse = begrunnelse,
        erSattPåVent = erSattPåVent,
        status = when (status) {
            VentestatusHendelseJson.Behandlingsstatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
            VentestatusHendelseJson.Behandlingsstatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
        },
    )
}

fun VentestatusHendelse.toDbJson(): VentestatusHendelseJson = VentestatusHendelseJson(
    begrunnelse = this.begrunnelse,
    endretAv = this.endretAv,
    tidspunkt = this.tidspunkt.toString(),
    erSattPåVent = this.erSattPåVent,
    status = when (this.status) {
        Behandlingsstatus.UNDER_BEHANDLING -> VentestatusHendelseJson.Behandlingsstatus.UNDER_BEHANDLING
        Behandlingsstatus.UNDER_BESLUTNING -> VentestatusHendelseJson.Behandlingsstatus.UNDER_BESLUTNING
        else -> throw IllegalArgumentException("Ugyldig behandlingsstatus for VentestatusHendelse: ${this.status}")
    },
)
