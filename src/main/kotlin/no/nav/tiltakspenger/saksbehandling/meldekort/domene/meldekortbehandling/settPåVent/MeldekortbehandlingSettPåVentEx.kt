package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent

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

fun Meldekortbehandling.settPåVent(
    kommando: SettMeldekortbehandlingPåVentKommando,
    clock: Clock,
): Meldekortbehandling {
    require(!ventestatus.erSattPåVent) { "Meldekortbehandling med id ${this.id} er allerede satt på vent" }
    val endretAv = kommando.saksbehandler

    return when (status) {
        UNDER_BEHANDLING -> {
            krevSaksbehandlerRolle(endretAv)
            require(this.saksbehandler == endretAv.navIdent) { "Du må være saksbehandler på meldekortbehandlingen for å kunne sette den på vent." }
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }

            val tidspunktSattPåVent = nå(clock)
            this.copy(
                ventestatus = ventestatus.settPåVent(
                    tidspunkt = tidspunktSattPåVent,
                    endretAv = endretAv.navIdent,
                    begrunnelse = kommando.begrunnelse,
                    status = status.toString(),
                    frist = kommando.frist,
                    clock = clock,
                ),
                saksbehandler = null,
                status = KLAR_TIL_BEHANDLING,
                sistEndret = tidspunktSattPåVent,
            )
        }

        UNDER_BESLUTNING -> {
            krevBeslutterRolle(endretAv)
            require(this.beslutter == endretAv.navIdent) { "Du må være beslutter på meldekortbehandlingen for å kunne sette den på vent." }
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }

            val tidspunktSattPåVent = nå(clock)
            this.copy(
                ventestatus = ventestatus.settPåVent(
                    tidspunkt = tidspunktSattPåVent,
                    endretAv = endretAv.navIdent,
                    begrunnelse = kommando.begrunnelse,
                    status = status.toString(),
                    frist = kommando.frist,
                    clock = clock,
                ),
                beslutter = null,
                status = KLAR_TIL_BESLUTNING,
                sistEndret = tidspunktSattPåVent,
            )
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> throw IllegalStateException("Kan ikke sette meldekortbehandling på vent som har status ${status.name}")
    }
}
