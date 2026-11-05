package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre

interface SokosUtbetaldataClient {
    /** Slår opp alle [fødselsnumre] personen har eller har hatt, og stopper ved første feil. */
    suspend fun hentYtelserFraUtbetaldata(
        fødselsnumre: Fødselsnumre,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, YtelserMedMetadata>
}
