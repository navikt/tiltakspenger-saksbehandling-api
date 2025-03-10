package no.nav.tiltakspenger.vedtak.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr

interface PoaoTilgangGateway {
    suspend fun erSkjermet(fnr: Fnr, correlationId: CorrelationId): Boolean
}
