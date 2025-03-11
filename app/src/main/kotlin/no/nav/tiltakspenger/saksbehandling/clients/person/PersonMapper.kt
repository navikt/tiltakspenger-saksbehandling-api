package no.nav.tiltakspenger.saksbehandling.clients.person

import arrow.core.getOrElse
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
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
import no.nav.tiltakspenger.libs.personklient.pdl.dto.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.GeografiskTilknytning
import no.nav.tiltakspenger.libs.personklient.pdl.dto.PdlPerson
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarFødsel
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarNavn
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.PersonopplysningerSøker

/**
 * Kopiert fra no.nav.tiltakspenger.vedtak.routes.rivers.PersonopplysningerRoutes.kt
 */
internal fun mapPersonopplysninger(
    json: String,
    fnr: Fnr,
): PersonopplysningerSøker {
    val data: PdlResponseData = objectMapper.readValue<PdlResponseData>(json)
    val person: PdlPerson = data.hentPerson
    val navn = avklarNavn(person.navn).getOrElse { it.mapError() }
    val fødsel = avklarFødsel(person.foedselsdato).getOrElse { it.mapError() }
    val adressebeskyttelse: AdressebeskyttelseGradering = avklarGradering(person.adressebeskyttelse).getOrElse { it.mapError() }
    return PersonopplysningerSøker(
        fnr = fnr,
        fødselsdato = fødsel.foedselsdato,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        fortrolig = adressebeskyttelse.erFortrolig(),
        strengtFortrolig = adressebeskyttelse.erStrengtFortrolig(),
        strengtFortroligUtland = adressebeskyttelse.erStrengtFortroligUtland(),
    )
}
private data class PdlResponseData(
    val hentGeografiskTilknytning: GeografiskTilknytning?,
    val hentPerson: PdlPerson,
)
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
