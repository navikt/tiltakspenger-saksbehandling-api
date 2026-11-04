package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode

interface SokosUtbetaldataClient {
    suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, YtelserMedMetadata>
}
