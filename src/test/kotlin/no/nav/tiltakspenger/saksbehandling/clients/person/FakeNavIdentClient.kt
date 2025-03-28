package no.nav.tiltakspenger.saksbehandling.clients.person

import no.nav.tiltakspenger.saksbehandling.oppgave.NavIdentClient

class FakeNavIdentClient : NavIdentClient {
    override suspend fun hentNavnForNavIdent(navIdent: String): String {
        return "Saksbehandler Saksbehandleren"
    }
}
