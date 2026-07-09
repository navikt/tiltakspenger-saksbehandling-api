package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.Either
import arrow.core.right

class FakeNavIdentClient : NavIdentClient {
    override suspend fun hentNavnForNavIdent(navIdent: String): Either<KanIkkeHenteNavnForNavIdent, String> {
        return "Saksbehandler Saksbehandleren".right()
    }
}
