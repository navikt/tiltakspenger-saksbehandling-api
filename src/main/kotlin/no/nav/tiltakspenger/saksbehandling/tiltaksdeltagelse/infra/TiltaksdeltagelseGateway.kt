package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse

interface TiltaksdeltagelseGateway {
    suspend fun hentTiltaksdeltagelse(fnr: Fnr, correlationId: CorrelationId): List<Tiltaksdeltagelse>
}
