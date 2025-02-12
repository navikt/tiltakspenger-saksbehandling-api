package no.nav.tiltakspenger.vedtak.clients.tiltak

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway

class TiltakGatewayImpl(
    private val tiltakClient: TiltakClient,
) : TiltakGateway {
    override suspend fun hentTiltaksdeltagelse(fnr: Fnr, maskerTiltaksnavn: Boolean, correlationId: CorrelationId): List<Tiltaksdeltagelse> = mapTiltak(tiltakClient.hentTiltak(fnr, correlationId), maskerTiltaksnavn)
}
