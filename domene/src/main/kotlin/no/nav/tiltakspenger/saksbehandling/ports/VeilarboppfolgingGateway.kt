package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.felles.Navkontor

interface VeilarboppfolgingGateway {
    suspend fun hentOppfolgingsenhet(fnr: String): Navkontor
}
