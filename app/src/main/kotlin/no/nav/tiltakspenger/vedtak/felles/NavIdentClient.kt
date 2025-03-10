package no.nav.tiltakspenger.vedtak.felles

interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): String
}
