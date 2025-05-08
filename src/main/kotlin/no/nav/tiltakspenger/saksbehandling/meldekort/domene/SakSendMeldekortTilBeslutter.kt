package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.sendMeldekortTilBeslutter(
    kommando: SendMeldekortTilBeslutterKommando,
    clock: Clock,
): Either<KanIkkeSendeMeldekortTilBeslutter, Pair<Sak, MeldekortBehandletManuelt>> {
    return this.meldekortBehandlinger.sendTilBeslutter(
        kommando = kommando,
        beregn = { meldeperiode ->
            this.beregn(
                meldekortIdSomBeregnes = kommando.meldekortId,
                // Denne vil kun kaste dersom denne funksjonen kalles og da må den være sendt med.
                meldeperiodeSomBeregnes = kommando.dager!!.tilMeldekortDager(meldeperiode),
            )
        },
        clock = clock,
    ).map { Pair(this.oppdaterMeldekortbehandling(it.second), it.second) }
}
