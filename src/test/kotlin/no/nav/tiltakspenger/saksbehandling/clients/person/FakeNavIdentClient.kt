package no.nav.tiltakspenger.saksbehandling.clients.person

import no.nav.tiltakspenger.saksbehandling.person.NavIdentClient

class FakeNavIdentClient : NavIdentClient {
    override suspend fun hentNavnForNavIdent(navIdent: String): String {
        return "Saksbehandler Saksbehandleren"
    }
}
