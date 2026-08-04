package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface MeldekortApiKlient {
    suspend fun sendSak(sak: Sak): Either<HttpKlientError, Unit>
}
