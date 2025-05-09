package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock

suspend fun Sak.sendMeldekortTilBeslutter(
    kommando: SendMeldekortTilBeslutterKommando,
    simuler: (suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
    clock: Clock,
): Either<KanIkkeSendeMeldekortTilBeslutter, Triple<Sak, MeldekortBehandletManuelt, SimuleringMedMetadata?>> {
    return this.meldekortBehandlinger.sendTilBeslutter(
        kommando = kommando,
        simuler = simuler,
        beregn = { meldeperiode ->
            this.beregn(
                meldekortIdSomBeregnes = kommando.meldekortId,
                // Denne vil kun kaste dersom denne funksjonen kalles og da må den være sendt med.
                meldeperiodeSomBeregnes = kommando.dager!!.tilMeldekortDager(meldeperiode),
            )
        },
        clock = clock,
    ).map { Triple(this.oppdaterMeldekortbehandlinger(it.first), it.second, it.third) }
}
