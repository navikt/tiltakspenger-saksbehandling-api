package no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

fun Rammebehandling.settPåVent(
    kommando: SettRammebehandlingPåVentKommando,
    clock: Clock,
): Either<KanIkkeSetteRammebehandlingPåVent, Pair<Rammebehandling, Statistikkhendelser>> {
    val endretAv = kommando.saksbehandler

    if (kommando.oppdaterPåVent) {
        kanOppdaterePåVent(endretAv).onLeft { return it.left() }
    } else {
        kanSettePåVent(endretAv).onLeft { return it.left() }
    }

    val (oppdatertSaksbehandler, oppdatertStatus) = when (status) {
        UNDER_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        -> null to KLAR_TIL_BEHANDLING

        UNDER_BESLUTNING,
        KLAR_TIL_BESLUTNING,
        -> saksbehandler to KLAR_TIL_BESLUTNING

        UNDER_AUTOMATISK_BEHANDLING -> saksbehandler to UNDER_AUTOMATISK_BEHANDLING

        VEDTATT,
        AVBRUTT,
        -> return KanIkkeSetteRammebehandlingPåVent.UgyldigStatus(status).left()
    }

    val nå = nå(clock)

    val oppdatertVentestatus = ventestatus.settPåVent(
        tidspunktSattPåVent = nå,
        endretAv = endretAv.navIdent,
        begrunnelse = kommando.begrunnelse,
        status = status.toString(),
        frist = kommando.frist,
    )

    val (oppdatertKlagebehandling, klagestatistikk) = oppdaterKlagebehandling(kommando, clock).getOrElse {
        return it.left()
    }

    val oppdatertRammebehandling = when (this) {
        is Søknadsbehandling -> {
            this.copy(
                ventestatus = oppdatertVentestatus,
                saksbehandler = oppdatertSaksbehandler,
                beslutter = null,
                status = oppdatertStatus,
                venterTil = kommando.venterTil,
                sistEndret = nå,
                klagebehandling = oppdatertKlagebehandling,
            )
        }

        is Revurdering ->
            this.copy(
                ventestatus = oppdatertVentestatus,
                saksbehandler = oppdatertSaksbehandler,
                beslutter = null,
                status = oppdatertStatus,
                venterTil = kommando.venterTil,
                sistEndret = nå,
                klagebehandling = oppdatertKlagebehandling,
            )
    }

    val statistikkhendelser = klagestatistikk.leggTil(
        oppdatertRammebehandling.genererSaksstatistikk(
            hendelse = StatistikkhendelseType.BEHANDLING_SATT_PA_VENT,
        ),
    )

    return (oppdatertRammebehandling to statistikkhendelser).right()
}

private fun Rammebehandling.oppdaterKlagebehandling(
    kommando: SettRammebehandlingPåVentKommando,
    clock: Clock,
): Either<KanIkkeSetteRammebehandlingPåVent, Pair<Klagebehandling?, Statistikkhendelser>> {
    val klage = klagebehandling ?: return (null to Statistikkhendelser.empty()).right()
    return klage.settPåVentOgNullstillSaksbehandler(
        kommando = SettKlagebehandlingPåVentKommando(
            sakId = kommando.sakId,
            klagebehandlingId = klage.id,
            saksbehandler = kommando.saksbehandler,
            oppdaterPåVent = kommando.oppdaterPåVent,
            begrunnelse = kommando.begrunnelse,
            frist = kommando.frist,
        ),
        clock = clock,
        sjekkSaksbehandler = this.status != UNDER_BESLUTNING,
    ).mapLeft { KanIkkeSetteRammebehandlingPåVent.KunneIkkeSetteKlagebehandlingenPåVent(it) }
}

/**
 * Avgjør om [saksbehandler] kan sette rammebehandlingen på vent.
 *
 * Betingelsene speiler hvilke tilstander [settPåVent] faktisk håndterer:
 *  - behandlingen kan ikke allerede være satt på vent
 *  - [UNDER_BEHANDLING]: kan settes på vent av saksbehandleren som er tildelt behandlingen
 *  - [UNDER_BESLUTNING]: kan settes på vent av beslutteren som er tildelt behandlingen
 *  - [UNDER_AUTOMATISK_BEHANDLING]: settes på vent av den automatiske saksbehandlingen, uten rollekrav
 */
fun Rammebehandling.kanSettePåVent(
    saksbehandler: Saksbehandler,
): Either<KanIkkeSetteRammebehandlingPåVent, Unit> {
    if (ventestatus.erSattPåVent) {
        return KanIkkeSetteRammebehandlingPåVent.BehandlingenErAlleredePåVent.left()
    }

    return when (status) {
        UNDER_AUTOMATISK_BEHANDLING -> Unit.right()

        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandlerForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutter.left()
            } else if (this.beslutter != saksbehandler.navIdent) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutterForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> KanIkkeSetteRammebehandlingPåVent.UgyldigStatus(status).left()
    }
}

/**
 * Avgjør om [saksbehandler] kan oppdatere begrunnelse og frist på en rammebehandling som allerede er satt på vent.
 *
 * Tilgangen er rollebasert, siden en behandling på vent ikke er tildelt noen i den fasen den venter i:
 *  - behandlingen må være satt på vent
 *  - [KLAR_TIL_BEHANDLING]: kan oppdateres av en saksbehandler
 *  - [KLAR_TIL_BESLUTNING]: kan oppdateres av en beslutter
 *  - [UNDER_AUTOMATISK_BEHANDLING]: kan oppdateres uten rollekrav
 */
fun Rammebehandling.kanOppdaterePåVent(
    saksbehandler: Saksbehandler,
): Either<KanIkkeSetteRammebehandlingPåVent, Unit> {
    if (!ventestatus.erSattPåVent) {
        return KanIkkeSetteRammebehandlingPåVent.BehandlingMåVæreSattPåVentForÅKunneOppdateres.left()
    }

    return when (status) {
        UNDER_AUTOMATISK_BEHANDLING -> Unit.right()

        KLAR_TIL_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING -> {
            if (!saksbehandler.erBeslutter) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutter.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> KanIkkeSetteRammebehandlingPåVent.UgyldigStatus(status).left()
    }
}
