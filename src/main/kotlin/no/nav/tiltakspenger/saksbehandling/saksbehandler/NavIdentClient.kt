package no.nav.tiltakspenger.saksbehandling.saksbehandler

interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): String
}
