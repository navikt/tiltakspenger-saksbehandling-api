package no.nav.tiltakspenger.saksbehandling.felles

interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): String
}
