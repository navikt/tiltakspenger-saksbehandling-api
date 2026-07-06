package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse

class SokosUtbetaldataFakeClient : SokosUtbetaldataClient {
    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Ytelse>> {
        return Either.Right(emptyList())
    }
}
