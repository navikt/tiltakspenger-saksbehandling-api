package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import java.time.Clock

fun MeldekortbehandlingManuell.oppdaterForBeslutter(
    kommando: OppdaterMeldekortbehandlingForBeslutterKommando,
    clock: Clock,
): Either<KanIkkeOppdatereMeldekortbehandlingForBeslutter, MeldekortbehandlingManuell> {
    kanOppdatereForBeslutter(kommando.saksbehandler, kommando.skalJournalføreNotat).onLeft { return it.left() }

    val tidspunkt = nå(clock)

    return this.copy(
        skalJournalføreNotat = kommando.skalJournalføreNotat,
        beslutterEndringer = beslutterEndringer.oppdater(
            gjeldendeValg = skalJournalføreNotat,
            besluttersValg = kommando.skalJournalføreNotat,
            tidspunkt = tidspunkt,
        ),
        sistEndret = tidspunkt,
    ).right()
}

private fun MeldekortbehandlingManuell.kanOppdatereForBeslutter(
    utøvendeBeslutter: Saksbehandler,
    nyttValg: Boolean,
): Either<KanIkkeOppdatereMeldekortbehandlingForBeslutter, Unit> {
    krevBeslutterRolle(utøvendeBeslutter)
    if (status != MeldekortbehandlingStatus.UNDER_BESLUTNING) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErIkkeUnderBeslutning.left()
    }
    if (beslutter != utøvendeBeslutter.navIdent) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.MåVæreBeslutterForMeldekortet.left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErSattPåVent.left()
    }
    if (nyttValg && begrunnelse == null) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.MåHaBegrunnelseForÅJournalføreNotat.left()
    }
    return Unit.right()
}
