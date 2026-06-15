package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.ta
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import java.time.Clock

/**
 * Tildeler [saksbehandler] som saksbehandler eller beslutter på meldekortbehandlingen avhengig av status:
 *  - [MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING] -> tildeles saksbehandler ([MeldekortbehandlingStatus.UNDER_BEHANDLING]).
 *  - [MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING] -> tildeles beslutter ([MeldekortbehandlingStatus.UNDER_BESLUTNING]).
 */
fun Meldekortbehandling.taMeldekortbehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeTaMeldekortbehandling, Meldekortbehandling> {
    kanTaMeldekortbehandling(saksbehandler).onLeft { return it.left() }

    val nå = nå(clock)
    return when (this.status) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) {
                "Forventet MeldekortUnderBehandling for status KLAR_TIL_BEHANDLING, var ${this::class.simpleName}"
            }

            this.copy(
                saksbehandler = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
                sistEndret = nå,
                klagebehandling = klagebehandling?.let { klage ->
                    klage.ta(
                        kommando = TaKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klage.id,
                            saksbehandler = saksbehandler,
                        ),
                        tilknyttetBehandlingsstatus = this.status.tilTilknyttetBehandlingsstatus(),
                        sistEndret = nå,
                    ).getOrThrow().first
                },
            ).right()
        }

        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) {
                "Forventet MeldekortbehandlingManuell for status KLAR_TIL_BESLUTNING, var ${this::class.simpleName}"
            }

            this.copy(
                beslutter = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
                sistEndret = nå,
            ).right()
        }

        MeldekortbehandlingStatus.UNDER_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BESLUTNING,
        MeldekortbehandlingStatus.GODKJENT,
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        MeldekortbehandlingStatus.AVBRUTT,
        -> KanIkkeTaMeldekortbehandling.UgyldigStatus(this.status).left()
    }
}

/**
 * Avgjør om [saksbehandler] kan ta meldekortbehandlingen.
 *
 * Betingelsene speiler hvilke tilstander [taMeldekortbehandling] faktisk håndterer:
 *  - [MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING]: kan tas av en saksbehandler dersom behandlingen ikke allerede har en saksbehandler.
 *  - [MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING]: kan tas av en beslutter (som ikke er saksbehandleren) dersom behandlingen ikke allerede har en beslutter.
 */
fun Meldekortbehandling.kanTaMeldekortbehandling(
    saksbehandler: Saksbehandler,
): Either<KanIkkeTaMeldekortbehandling, Unit> {
    return when (this.status) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
            if (this.saksbehandler != null) {
                KanIkkeTaMeldekortbehandling.HarAlleredeSaksbehandler.left()
            } else if (!saksbehandler.erSaksbehandler()) {
                KanIkkeTaMeldekortbehandling.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
            if (this.beslutter != null) {
                KanIkkeTaMeldekortbehandling.HarAlleredeBeslutter.left()
            } else if (this.saksbehandler == saksbehandler.navIdent) {
                KanIkkeTaMeldekortbehandling.BeslutterKanIkkeVæreSammeSomSaksbehandler.left()
            } else if (!saksbehandler.erBeslutter()) {
                KanIkkeTaMeldekortbehandling.MåVæreBeslutter.left()
            } else {
                Unit.right()
            }
        }

        MeldekortbehandlingStatus.UNDER_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BESLUTNING,
        MeldekortbehandlingStatus.GODKJENT,
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        MeldekortbehandlingStatus.AVBRUTT,
        -> KanIkkeTaMeldekortbehandling.UgyldigStatus(this.status).left()
    }
}
