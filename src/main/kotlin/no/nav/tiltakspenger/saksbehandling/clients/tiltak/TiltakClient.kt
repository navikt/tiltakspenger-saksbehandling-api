package no.nav.tiltakspenger.saksbehandling.clients.tiltak

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO

interface TiltakClient {
    suspend fun hentTiltak(fnr: Fnr, correlationId: CorrelationId): List<TiltakTilSaksbehandlingDTO>
}
