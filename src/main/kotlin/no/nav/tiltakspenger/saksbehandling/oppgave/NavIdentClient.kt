package no.nav.tiltakspenger.saksbehandling.oppgave

interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): String
}
