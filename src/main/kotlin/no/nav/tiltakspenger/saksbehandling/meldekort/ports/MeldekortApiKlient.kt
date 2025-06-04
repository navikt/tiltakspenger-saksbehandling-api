package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface MeldekortApiKlient {
    suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit>
}
data object FeilVedSendingTilMeldekortApi
