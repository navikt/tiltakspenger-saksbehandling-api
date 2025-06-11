package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse

class SokosUtbetaldataFakeClient : SokosUtbetaldataClient {
    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): List<Ytelse> {
        return emptyList()
    }
}
