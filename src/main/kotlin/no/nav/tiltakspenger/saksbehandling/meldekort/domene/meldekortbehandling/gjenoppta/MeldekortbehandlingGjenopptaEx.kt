package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BESLUTNING
import java.time.Clock

fun Meldekortbehandling.gjenoppta(
    kommando: GjenopptaMeldekortbehandlingKommando,
    clock: Clock,
): Meldekortbehandling {
    require(ventestatus.erSattPåVent) { "Meldekortbehandling med id ${this.id} er ikke satt på vent" }
    val gjenopptattAv = kommando.saksbehandler
    val nå = nå(clock)
    val oppdatertVentestatus = ventestatus.gjenoppta(
        tidspunkt = nå,
        endretAv = gjenopptattAv.navIdent,
        status = status.toString(),
    )

    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            krevSaksbehandlerRolle(gjenopptattAv)
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            this.copy(
                saksbehandler = gjenopptattAv.navIdent,
                status = UNDER_BEHANDLING,
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
            )
        }

        UNDER_BEHANDLING -> {
            krevSaksbehandlerRolle(gjenopptattAv)
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            require(this.saksbehandler == gjenopptattAv.navIdent) {
                "Du må være saksbehandler på meldekortbehandlingen for å kunne gjenoppta den."
            }
            this.copy(
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
            )
        }

        KLAR_TIL_BESLUTNING -> {
            krevBeslutterRolle(gjenopptattAv)
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }
            require(this.saksbehandler != gjenopptattAv.navIdent) {
                "Beslutter (${gjenopptattAv.navIdent}) kan ikke være den samme som saksbehandleren (${this.saksbehandler})"
            }
            this.copy(
                beslutter = gjenopptattAv.navIdent,
                status = UNDER_BESLUTNING,
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
            )
        }

        UNDER_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> throw IllegalStateException("Kan ikke gjenoppta meldekortbehandling som har status ${status.name}")
    }
}
