package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class MeldekortApiFakeGateway : MeldekortApiKlient {
    override suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Unit.right()
    }
}
