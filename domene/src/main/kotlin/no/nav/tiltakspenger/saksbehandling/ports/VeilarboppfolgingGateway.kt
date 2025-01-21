package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.Fnr

interface VeilarboppfolgingGateway {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor
}
