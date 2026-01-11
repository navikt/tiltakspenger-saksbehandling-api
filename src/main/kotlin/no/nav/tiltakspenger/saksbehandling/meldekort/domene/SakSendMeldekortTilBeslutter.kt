package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.sendMeldekortTilBeslutter(
    kommando: SendMeldekortTilBeslutterKommando,
    clock: Clock,
): Either<KanIkkeSendeMeldekortTilBeslutter, Pair<Sak, MeldekortBehandletManuelt>> {
    return this.meldekortbehandlinger.sendTilBeslutter(
        kommando = kommando,
        clock = clock,
    ).map { Pair(this.oppdaterMeldekortbehandlinger(it.first), it.second) }
}
