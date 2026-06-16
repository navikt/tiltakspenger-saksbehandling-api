package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.LeggTilbakeKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbake
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
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

/**
 * Legger tilbake meldekortbehandlingen for [saksbehandler]:
 *  - [UNDER_BEHANDLING] -> nullstiller saksbehandler ([KLAR_TIL_BEHANDLING]).
 *  - [UNDER_BESLUTNING] -> nullstiller beslutter ([KLAR_TIL_BESLUTNING]).
 */
fun Meldekortbehandling.leggTilbakeMeldekortbehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeLeggeTilbakeMeldekortbehandling, Meldekortbehandling> {
    kanLeggeTilbake(saksbehandler).onLeft { return it.left() }

    return when (status) {
        UNDER_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            this.copy(
                saksbehandler = null,
                status = KLAR_TIL_BEHANDLING,
                sistEndret = nå(clock),
                klagebehandling = klagebehandling?.let { klage ->
                    klage.leggTilbake(
                        kommando = LeggTilbakeKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klage.id,
                            saksbehandler = saksbehandler,
                        ),
                        tilknyttetBehandlingsstatus = status.tilTilknyttetBehandlingsstatus(),
                        clock = clock,
                    ).getOrThrow().first
                },
            ).right()
        }

        UNDER_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }
            this.copy(
                beslutter = null,
                status = KLAR_TIL_BESLUTNING,
                sistEndret = nå(clock),
            ).right()
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanLeggeTilbake. Kan ikke legge tilbake meldekortbehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan legge tilbake meldekortbehandlingen.
 *
 * Betingelsene speiler hvilke tilstander [leggTilbakeMeldekortbehandling] faktisk håndterer:
 *  - [UNDER_BEHANDLING]: kan legges tilbake av saksbehandleren som er tildelt behandlingen.
 *  - [UNDER_BESLUTNING]: kan legges tilbake av beslutteren som er tildelt behandlingen.
 */
fun Meldekortbehandling.kanLeggeTilbake(
    saksbehandler: Saksbehandler,
): Either<KanIkkeLeggeTilbakeMeldekortbehandling, Unit> {
    return when (status) {
        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreSaksbehandlerForMeldekortet.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreBeslutter.left()
            } else if (this.beslutter != saksbehandler.navIdent) {
                KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreBeslutterForMeldekortet.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> KanIkkeLeggeTilbakeMeldekortbehandling.UgyldigStatus(status).left()
    }
}
