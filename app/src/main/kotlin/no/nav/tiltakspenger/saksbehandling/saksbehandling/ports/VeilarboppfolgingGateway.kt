package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.felles.Navkontor

interface VeilarboppfolgingGateway {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor
}
