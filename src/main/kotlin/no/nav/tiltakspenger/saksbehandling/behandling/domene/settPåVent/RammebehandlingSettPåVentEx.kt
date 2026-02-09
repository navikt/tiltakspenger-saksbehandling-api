package no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVent
import java.time.Clock

fun Rammebehandling.settPåVent(
    kommando: SettRammebehandlingPåVentKommando,
    clock: Clock,
): Rammebehandling {
    require(!ventestatus.erSattPåVent) { "Behandling med id ${this.id} er allerede satt på vent" }
    val endretAv = kommando.saksbehandler
    when (status) {
        UNDER_AUTOMATISK_BEHANDLING,
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        -> {
            if (status == UNDER_BEHANDLING) {
                krevSaksbehandlerRolle(endretAv)
                require(this.saksbehandler == endretAv.navIdent) { "Du må være saksbehandler på behandlingen for å kunne sette den på vent." }
            }
            if (status == UNDER_BESLUTNING) {
                krevBeslutterRolle(endretAv)
                require(this.beslutter == endretAv.navIdent) { "Du må være beslutter på behandlingen for å kunne sette den på vent." }
            }

            val oppdatertSaksbehandler = if (status == UNDER_AUTOMATISK_BEHANDLING || status == UNDER_BESLUTNING) {
                saksbehandler
            } else {
                null
            }

            val oppdatertStatus = when (status) {
                UNDER_BESLUTNING -> KLAR_TIL_BESLUTNING
                UNDER_BEHANDLING -> KLAR_TIL_BEHANDLING
                UNDER_AUTOMATISK_BEHANDLING -> UNDER_AUTOMATISK_BEHANDLING
                else -> throw IllegalStateException("Uventet status $status ved oppdatering til ventende status")
            }
            val oppdatertVentestatus = ventestatus.settPåVent(
                tidspunkt = nå(clock),
                endretAv = endretAv.navIdent,
                begrunnelse = kommando.begrunnelse,
                status = status.toString(),
                frist = kommando.frist,
            )
            val oppdatertKlagebehandling =
                if (klagebehandling != null && status == UNDER_BEHANDLING) {
                    klagebehandling!!.settPåVent(
                        kommando = SettKlagebehandlingPåVentKommando(
                            sakId = kommando.sakId,
                            klagebehandlingId = klagebehandling!!.id,
                            saksbehandler = kommando.saksbehandler,
                            begrunnelse = kommando.begrunnelse,
                            frist = kommando.frist,
                        ),
                        clock = clock,
                    ).getOrThrow()
                } else {
                    klagebehandling
                }
            return when (this) {
                is Søknadsbehandling -> {
                    this.copy(
                        ventestatus = oppdatertVentestatus,
                        saksbehandler = oppdatertSaksbehandler,
                        beslutter = null,
                        status = oppdatertStatus,
                        venterTil = kommando.venterTil,
                        sistEndret = nå(clock),
                        klagebehandling = oppdatertKlagebehandling,
                    )
                }

                is Revurdering -> {
                    this.copy(
                        ventestatus = oppdatertVentestatus,
                        saksbehandler = oppdatertSaksbehandler,
                        beslutter = null,
                        status = oppdatertStatus,
                        venterTil = kommando.venterTil,
                        sistEndret = nå(clock),
                        klagebehandling = oppdatertKlagebehandling,
                    )
                }
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> throw IllegalStateException("Kan ikke sette behandling på vent som har status ${status.name}")
    }
}
