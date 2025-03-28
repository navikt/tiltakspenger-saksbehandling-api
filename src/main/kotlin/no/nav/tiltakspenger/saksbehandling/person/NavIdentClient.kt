package no.nav.tiltakspenger.saksbehandling.person

interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): String
}
