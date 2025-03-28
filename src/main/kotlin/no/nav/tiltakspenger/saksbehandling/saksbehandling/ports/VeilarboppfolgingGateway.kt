package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor

interface VeilarboppfolgingGateway {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor
}
