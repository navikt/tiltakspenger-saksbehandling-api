package no.nav.tiltakspenger.saksbehandling.ports

interface NorgGateway {
    suspend fun hentNavkontor(enhetsnummer: String): String
}
