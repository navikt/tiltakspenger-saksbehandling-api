package no.nav.tiltakspenger.clients.person

import no.nav.tiltakspenger.vedtak.felles.NavIdentClient

class FakeNavIdentClient : NavIdentClient {
    override suspend fun hentNavnForNavIdent(navIdent: String): String {
        return "Saksbehandler Saksbehandleren"
    }
}
