package no.nav.tiltakspenger.vedtak.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.vedtak.felles.Navkontor

interface VeilarboppfolgingGateway {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor
}
