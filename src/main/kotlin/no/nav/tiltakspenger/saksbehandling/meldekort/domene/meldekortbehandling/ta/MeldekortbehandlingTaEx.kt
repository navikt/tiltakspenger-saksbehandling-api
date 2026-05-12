package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
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
    return when (this.status) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) {
                "Forventet MeldekortUnderBehandling for status KLAR_TIL_BEHANDLING, var ${this::class.simpleName}"
            }

            if (this.saksbehandler != null) {
                return KanIkkeTaMeldekortbehandling.HarAlleredeSaksbehandler.left()
            }

            krevSaksbehandlerRolle(saksbehandler)

            this.copy(
                saksbehandler = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
                sistEndret = nå(clock),
            ).right()
        }

        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) {
                "Forventet MeldekortbehandlingManuell for status KLAR_TIL_BESLUTNING, var ${this::class.simpleName}"
            }

            if (this.beslutter != null) {
                return KanIkkeTaMeldekortbehandling.HarAlleredeBeslutter.left()
            }

            if (this.saksbehandler == saksbehandler.navIdent) {
                return KanIkkeTaMeldekortbehandling.BeslutterKanIkkeVæreSammeSomSaksbehandler.left()
            }

            krevBeslutterRolle(saksbehandler)

            this.copy(
                beslutter = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
                sistEndret = nå(clock),
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
