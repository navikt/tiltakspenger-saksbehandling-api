package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway

class MeldekortApiFakeGateway : MeldekortApiHttpClientGateway {
    override suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Unit.right()
    }
}
