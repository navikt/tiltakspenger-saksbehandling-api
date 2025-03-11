package no.nav.tiltakspenger.saksbehandling.clients.tiltak

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.TiltakGateway

class TiltakGatewayImpl(
    private val tiltakClient: TiltakClient,
) : TiltakGateway {
    override suspend fun hentTiltaksdeltagelse(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): List<Tiltaksdeltagelse> {
        val tiltak = tiltakClient.hentTiltak(fnr, correlationId)
        val relevanteTiltak = tiltak.filter { it.harFomOgTomEllerRelevantStatus() }
            .filter { it.rettPaTiltakspenger() }
        return mapTiltak(relevanteTiltak)
    }
}
