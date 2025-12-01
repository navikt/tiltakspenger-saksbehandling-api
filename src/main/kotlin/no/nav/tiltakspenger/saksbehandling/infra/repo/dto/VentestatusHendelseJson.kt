package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
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
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        UNDER_BESLUTNING,
        UNDER_AUTOMATISK_BEHANDLING,
    }
}

fun VentestatusHendelseJson.toSattPåVentBegrunnelse(): VentestatusHendelse {
    return VentestatusHendelse(
        tidspunkt = LocalDateTime.parse(tidspunkt),
        endretAv = endretAv,
        begrunnelse = begrunnelse,
        erSattPåVent = erSattPåVent,
        status = when (status) {
            VentestatusHendelseJson.Behandlingsstatus.KLAR_TIL_BEHANDLING -> Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            VentestatusHendelseJson.Behandlingsstatus.UNDER_BEHANDLING -> Rammebehandlingsstatus.UNDER_BEHANDLING
            VentestatusHendelseJson.Behandlingsstatus.KLAR_TIL_BESLUTNING -> Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            VentestatusHendelseJson.Behandlingsstatus.UNDER_BESLUTNING -> Rammebehandlingsstatus.UNDER_BESLUTNING
            VentestatusHendelseJson.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        },
    )
}

fun VentestatusHendelse.toDbJson(): VentestatusHendelseJson = VentestatusHendelseJson(
    begrunnelse = this.begrunnelse,
    endretAv = this.endretAv,
    tidspunkt = this.tidspunkt.toString(),
    erSattPåVent = this.erSattPåVent,
    status = when (this.status) {
        Rammebehandlingsstatus.UNDER_BEHANDLING -> VentestatusHendelseJson.Behandlingsstatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> VentestatusHendelseJson.Behandlingsstatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> VentestatusHendelseJson.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> VentestatusHendelseJson.Behandlingsstatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> VentestatusHendelseJson.Behandlingsstatus.KLAR_TIL_BESLUTNING
        else -> throw IllegalArgumentException("Ugyldig behandlingsstatus for VentestatusHendelse: ${this.status}")
    },
)
