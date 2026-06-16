package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
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
): Either<KanIkkeGjenopptaMeldekortbehandling, Meldekortbehandling> {
    kanGjenoppta(kommando.saksbehandler).onLeft { return it.left() }

    val gjenopptattAv = kommando.saksbehandler
    val nå = nå(clock)
    val oppdatertVentestatus = ventestatus.gjenoppta(
        tidspunkt = nå,
        endretAv = gjenopptattAv.navIdent,
        status = status.toString(),
    )
    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            this.copy(
                saksbehandler = gjenopptattAv.navIdent,
                status = UNDER_BEHANDLING,
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
                klagebehandling = klagebehandling?.let { klage ->
                    klage.gjenopptaKlagebehandling(
                        kommando = GjenopptaKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klage.id,
                            saksbehandler = kommando.saksbehandler,
                            correlationId = kommando.correlationId,
                        ),
                        clock = clock,
                    ).getOrThrow().first
                },
            ).right()
        }

        UNDER_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            this.copy(
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
                klagebehandling = klagebehandling?.let { klage ->
                    klage.gjenopptaKlagebehandling(
                        kommando = GjenopptaKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klage.id,
                            saksbehandler = kommando.saksbehandler,
                            correlationId = kommando.correlationId,
                        ),
                        clock = clock,
                    ).getOrThrow().first
                },
            ).right()
        }

        KLAR_TIL_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }
            this.copy(
                beslutter = gjenopptattAv.navIdent,
                status = UNDER_BESLUTNING,
                ventestatus = oppdatertVentestatus,
                sistEndret = nå,
                // klage har ikke noe forhold til beslutter, derfor gjenbruker vi klagens saksbehandler ved gjenoppta når meldekortbehandlingen er klar til/under beslutning
                klagebehandling = klagebehandling?.gjenopptaKlagebehandling(
                    klagensSaksbehandler = this.saksbehandler,
                    endretAv = kommando.saksbehandler.navIdent,
                    clock = clock,
                )?.getOrThrow()?.first,
            ).right()
        }

        UNDER_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeGjenopptaMeldekortbehandling.UgyldigStatus(status).left()
    }
}

/**
 * Avgjør om meldekortbehandlingen kan gjenopptas av [saksbehandler].
 *
 * Betingelsene speiler hvilke tilstander [gjenoppta] faktisk håndterer:
 *  - behandlingen må være satt på vent
 *  - [KLAR_TIL_BEHANDLING]: kan gjenopptas av en saksbehandler
 *  - [UNDER_BEHANDLING]: kan gjenopptas av saksbehandleren som er tildelt behandlingen
 *  - [KLAR_TIL_BESLUTNING]: kan gjenopptas av en beslutter som ikke er saksbehandleren på behandlingen
 */
fun Meldekortbehandling.kanGjenoppta(
    saksbehandler: Saksbehandler,
): Either<KanIkkeGjenopptaMeldekortbehandling, Unit> {
    if (!ventestatus.erSattPåVent) {
        return KanIkkeGjenopptaMeldekortbehandling.BehandlingenErIkkePåVent.left()
    }
    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KanIkkeGjenopptaMeldekortbehandling.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KanIkkeGjenopptaMeldekortbehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeGjenopptaMeldekortbehandling.MåVæreSaksbehandlerSomEierBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KanIkkeGjenopptaMeldekortbehandling.MåVæreBeslutter.left()
            } else if (this.saksbehandler == saksbehandler.navIdent) {
                KanIkkeGjenopptaMeldekortbehandling.BeslutterKanIkkeVæreSammeSomSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeGjenopptaMeldekortbehandling.UgyldigStatus(status).left()
    }
}
