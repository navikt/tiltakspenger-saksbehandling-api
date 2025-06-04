package no.nav.tiltakspenger.saksbehandling.saksbehandler

class FakeNavIdentClient : NavIdentClient {
    override suspend fun hentNavnForNavIdent(navIdent: String): String {
        return "Saksbehandler Saksbehandleren"
    }
}
