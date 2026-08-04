package no.nav.tiltakspenger.saksbehandling.person.infra.http

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.AdressebeskyttelseKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.DeserializationException
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.FantIkkePerson
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.FødselKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.Ikke2xx
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.IngenNavnFunnet
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.Kallfeil
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.NavnKunneIkkeAvklares
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.NetworkError
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.ResponsManglerData
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError.UkjentFeil

private val logger = KotlinLogging.logger {}

/**
 * PDL-klienten logger ikke selv, så feilen må logges her før den kastes videre.
 *
 * For [Kallfeil] gjøres det med `loggFeil`, som får med feilart, endepunkt, antall forsøk og varighet — kontekst som ikke finnes noe annet sted.
 * En `HttpConnectTimeoutException` lages på JDK-ens egen I/O-tråd, så exceptionen vi kaster videre herfra bærer en stacktrace uten en eneste applikasjonsframe.
 * De øvrige variantene er utledet av et svar vi forsto, og har ingen HTTP-kontekst å logge.
 */
fun FellesPersonklientError.mapError(): Nothing {
    if (this is Kallfeil) {
        httpKlientError.loggFeil(
            logger = logger,
            operasjon = "henting av personopplysninger fra PDL",
            // PDL-oppslaget skjer på fnr, som aldri kan i vanlig logg, og klienten sender foreløpig ingen correlation id (se TODO i FellesHttpPersonklient).
            kontekst = "Ingen domenekontekst tilgjengelig",
        )
    }
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

        is Ikke2xx -> throw RuntimeException("Feil ved henting av personopplysninger: Ikke2xx, status ${this.status}")

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
