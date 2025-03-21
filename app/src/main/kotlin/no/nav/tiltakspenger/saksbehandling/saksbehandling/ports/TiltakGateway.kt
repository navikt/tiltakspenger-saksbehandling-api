package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse

interface TiltakGateway {
    suspend fun hentTiltaksdeltagelse(fnr: Fnr, correlationId: CorrelationId): List<Tiltaksdeltagelse>
}
