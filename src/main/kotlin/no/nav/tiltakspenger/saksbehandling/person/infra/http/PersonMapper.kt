package no.nav.tiltakspenger.saksbehandling.person.infra.http

import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.AdressebeskyttelseKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.DeserializationException
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.FantIkkePerson
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.FødselKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.Ikke2xx
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.IngenNavnFunnet
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.NavnKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.NetworkError
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.ResponsManglerData
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.UkjentFeil

internal fun FellesPersonklientError.mapError(): Nothing {
    when (this) {
        is AdressebeskyttelseKunneIkkeAvklares -> throw RuntimeException(
            "Feil ved henting av personopplysninger: AdressebeskyttelseKunneIkkeAvklares",
        )
        is DeserializationException -> throw RuntimeException(
            "Feil ved henting av personopplysninger: DeserializationException",
            this.exception,
        )

        is FantIkkePerson -> throw RuntimeException("Feil ved henting av personopplysninger: FantIkkePerson")
        is FødselKunneIkkeAvklares -> throw RuntimeException("Feil ved henting av personopplysninger: FødselKunneIkkeAvklares")
        is Ikke2xx -> throw RuntimeException("Feil ved henting av personopplysninger: $this")
        is IngenNavnFunnet -> throw RuntimeException("Feil ved henting av personopplysninger: IngenNavnFunnet")
        is NavnKunneIkkeAvklares -> throw RuntimeException("Feil ved henting av personopplysninger: NavnKunneIkkeAvklares")
        is NetworkError -> throw RuntimeException(
            "Feil ved henting av personopplysninger: NetworkError",
            this.exception,
        )

        is ResponsManglerData -> throw RuntimeException("Feil ved henting av personopplysninger: ResponsManglerPerson")
        is UkjentFeil -> throw RuntimeException("Feil ved henting av personopplysninger: $this")
    }
}
