package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler
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
): Either<KanIkkeSetteMeldekortbehandlingPåVent, Meldekortbehandling> {
    if (kommando.oppdaterPåVent) {
        kanOppdaterePåVent(kommando.saksbehandler).onLeft { return it.left() }
    } else {
        kanSettePåVent(kommando.saksbehandler).onLeft { return it.left() }
    }

    val endretAv = kommando.saksbehandler

    return when (status) {
        UNDER_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        -> {
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            val tidspunktSattPåVent = nå(clock)
            val oppdatertKlagebehandling = klagebehandling?.let { klage ->
                klage.settPåVentOgNullstillSaksbehandler(
                    kommando = SettKlagebehandlingPåVentKommando(
                        sakId = sakId,
                        klagebehandlingId = klage.id,
                        saksbehandler = kommando.saksbehandler,
                        oppdaterPåVent = kommando.oppdaterPåVent,
                        begrunnelse = kommando.begrunnelse,
                        frist = kommando.frist,
                    ),
                    clock = clock,
                    sjekkSaksbehandler = true,
                ).getOrElse {
                    return KanIkkeSetteMeldekortbehandlingPåVent.KunneIkkeSetteKlagebehandlingenPåVent(it).left()
                }.first
            }
            this.copy(
                ventestatus = ventestatus.settPåVent(
                    tidspunktSattPåVent = tidspunktSattPåVent,
                    endretAv = endretAv.navIdent,
                    begrunnelse = kommando.begrunnelse,
                    status = status.toString(),
                    frist = kommando.frist,
                ),
                saksbehandler = null,
                status = KLAR_TIL_BEHANDLING,
                sistEndret = tidspunktSattPåVent,
                klagebehandling = oppdatertKlagebehandling,
            ).right()
        }

        UNDER_BESLUTNING,
        KLAR_TIL_BESLUTNING,
        -> {
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }

            val tidspunktSattPåVent = nå(clock)
            val oppdatertKlagebehandling = klagebehandling?.let { klage ->
                klage.settPåVent(
                    kommando = SettKlagebehandlingPåVentKommando(
                        sakId = sakId,
                        klagebehandlingId = klage.id,
                        saksbehandler = kommando.saksbehandler,
                        oppdaterPåVent = kommando.oppdaterPåVent,
                        begrunnelse = kommando.begrunnelse,
                        frist = kommando.frist,
                    ),
                    clock = clock,
                    sjekkSaksbehandler = false,
                ).getOrElse {
                    return KanIkkeSetteMeldekortbehandlingPåVent.KunneIkkeSetteKlagebehandlingenPåVent(it).left()
                }.first
            }
            this.copy(
                ventestatus = ventestatus.settPåVent(
                    tidspunktSattPåVent = tidspunktSattPåVent,
                    endretAv = endretAv.navIdent,
                    begrunnelse = kommando.begrunnelse,
                    status = status.toString(),
                    frist = kommando.frist,
                ),
                beslutter = null,
                status = KLAR_TIL_BESLUTNING,
                sistEndret = tidspunktSattPåVent,
                klagebehandling = oppdatertKlagebehandling,
            ).right()
        }

        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeSetteMeldekortbehandlingPåVent.UgyldigStatus(status).left()
    }
}

/**
 * Avgjør om [saksbehandler] kan sette meldekortbehandlingen på vent.
 *
 * Betingelsene speiler hvilke tilstander [settPåVent] faktisk håndterer:
 *  - behandlingen kan ikke allerede være satt på vent
 *  - [UNDER_BEHANDLING]: kan settes på vent av saksbehandleren som er tildelt behandlingen
 *  - [UNDER_BESLUTNING]: kan settes på vent av beslutteren som er tildelt behandlingen
 */
fun Meldekortbehandling.kanSettePåVent(
    saksbehandler: Saksbehandler,
): Either<KanIkkeSetteMeldekortbehandlingPåVent, Unit> {
    if (ventestatus.erSattPåVent) {
        return KanIkkeSetteMeldekortbehandlingPåVent.BehandlingenErAlleredePåVent.left()
    }

    return when (status) {
        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreSaksbehandlerForMeldekortet.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreBeslutter.left()
            } else if (this.beslutter != saksbehandler.navIdent) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreBeslutterForMeldekortet.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeSetteMeldekortbehandlingPåVent.UgyldigStatus(status).left()
    }
}

/**
 * Avgjør om [saksbehandler] kan oppdatere begrunnelse og frist på en meldekortbehandling som allerede er satt på vent.
 *
 * Tilgangen er rollebasert, siden en behandling på vent ikke er tildelt noen i den fasen den venter i:
 *  - behandlingen må være satt på vent
 *  - [KLAR_TIL_BEHANDLING]: kan oppdateres av en saksbehandler
 *  - [KLAR_TIL_BESLUTNING]: kan oppdateres av en beslutter
 */
fun Meldekortbehandling.kanOppdaterePåVent(
    saksbehandler: Saksbehandler,
): Either<KanIkkeSetteMeldekortbehandlingPåVent, Unit> {
    if (!ventestatus.erSattPåVent) {
        return KanIkkeSetteMeldekortbehandlingPåVent.BehandlingMåVæreSattPåVentForÅKunneOppdateres.left()
    }

    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING -> {
            if (!saksbehandler.erBeslutter) {
                KanIkkeSetteMeldekortbehandlingPåVent.MåVæreBeslutter.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeSetteMeldekortbehandlingPåVent.UgyldigStatus(status).left()
    }
}
