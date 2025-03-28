package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseGateway

class TiltaksdeltagelseGatewayImpl(
    private val tiltaksdeltagelseHttpklient: TiltaksdeltagelseHttpklient,
) : TiltaksdeltagelseGateway {
    override suspend fun hentTiltaksdeltagelse(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): List<Tiltaksdeltagelse> {
        val tiltak = tiltaksdeltagelseHttpklient.hentTiltaksdeltagelser(fnr, correlationId)
        val relevanteTiltak = tiltak.filter { it.harFomOgTomEllerRelevantStatus() }
            .filter { it.rettPaTiltakspenger() }
        return mapTiltak(relevanteTiltak)
    }
}
