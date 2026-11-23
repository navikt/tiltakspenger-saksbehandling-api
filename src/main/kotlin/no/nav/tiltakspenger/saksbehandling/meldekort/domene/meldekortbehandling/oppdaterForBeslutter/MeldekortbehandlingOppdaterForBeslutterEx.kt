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

/**
 * Frontend lar saksbehandler og beslutter velge journalføring og utsending av vedtaksbrevet samlet.
 * Beslutters endring av det samlede valget spores derfor kun som en endring av utsendingen.
 */
fun MeldekortbehandlingManuell.oppdaterForBeslutter(
    kommando: OppdaterMeldekortbehandlingForBeslutterKommando,
    clock: Clock,
): Either<KanIkkeOppdatereMeldekortbehandlingForBeslutter, MeldekortbehandlingManuell> {
    val nyttJournalføringsvalg = kommando.skalJournalføreVedtaksbrev
    val nyttDistribusjonsvalg = kommando.skalDistribuereVedtaksbrev
    kanOppdatereForBeslutter(
        utøvendeBeslutter = kommando.saksbehandler,
        nyttNotatvalg = kommando.skalJournalføreNotat,
        nyttJournalføringsvalg = nyttJournalføringsvalg,
        nyttDistribusjonsvalg = nyttDistribusjonsvalg,
    ).onLeft { return it.left() }

    val tidspunkt = nå(clock)

    return this.copy(
        skalJournalføreNotat = kommando.skalJournalføreNotat,
        skalJournalføreVedtaksbrev = nyttJournalføringsvalg,
        skalDistribuereVedtaksbrev = nyttDistribusjonsvalg,
        beslutterEndringer = beslutterEndringer
            .oppdaterSkalJournalføreNotat(
                gjeldendeValg = skalJournalføreNotat,
                besluttersValg = kommando.skalJournalføreNotat,
                tidspunkt = tidspunkt,
            )
            .oppdaterSkalDistribuereVedtaksbrev(
                gjeldendeValg = skalDistribuereVedtaksbrev,
                besluttersValg = nyttDistribusjonsvalg,
                tidspunkt = tidspunkt,
            ),
        sistEndret = tidspunkt,
    ).right()
}

private fun MeldekortbehandlingManuell.kanOppdatereForBeslutter(
    utøvendeBeslutter: Saksbehandler,
    nyttNotatvalg: Boolean,
    nyttJournalføringsvalg: Boolean,
    nyttDistribusjonsvalg: Boolean,
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
    if (nyttNotatvalg && begrunnelse == null) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.MåHaBegrunnelseForÅJournalføreNotat.left()
    }
    if (nyttDistribusjonsvalg && !nyttJournalføringsvalg) {
        return KanIkkeOppdatereMeldekortbehandlingForBeslutter.KanIkkeDistribuereVedtaksbrevUtenJournalføring.left()
    }
    return Unit.right()
}
