package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.ports.PersonGateway

class PersonFakeGateway : PersonGateway {
    private val data = Atomic(mutableMapOf<Fnr, PersonopplysningerSøker>())

    private val kall = Atomic(mutableListOf<Fnr>())
    val antallKall: Int get() = kall.get().size
    val alleKall: List<Fnr> get() = kall.get().toList()

    override suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker =
        data.get()[fnr] ?: personopplysningerSøkerDefault(fnr)

    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson = data.get()[fnr]?.let {
        EnkelPerson(
            fnr = fnr,
            fornavn = it.fornavn,
            mellomnavn = it.mellomnavn,
            etternavn = it.etternavn,
            fortrolig = it.fortrolig,
            strengtFortrolig = it.strengtFortrolig,
            strengtFortroligUtland = it.strengtFortroligUtland,
        )
    } ?: enkelPersonDefault(fnr)

    /**
     * Denne bør kalles av testoppsettet før vi lager en søknad.
     * Overskriver eksisterende personopplysninger for personen.
     */
    fun leggTilPersonopplysning(
        fnr: Fnr,
        personopplysninger: PersonopplysningerSøker,
    ) {
        data.get()[fnr] = personopplysninger
    }

    private fun enkelPersonDefault(fnr: Fnr): EnkelPerson {
        return EnkelPerson(
            fnr = fnr,
            fornavn = "Navny",
            mellomnavn = null,
            etternavn = "McNavnface",
            fortrolig = false,
            strengtFortrolig = false,
            strengtFortroligUtland = false,
        )
    }

    private fun personopplysningerSøkerDefault(fnr: Fnr): PersonopplysningerSøker {
        return PersonopplysningerSøker(
            fnr = fnr,
            fødselsdato = nå().toLocalDate().minusYears(20),
            fornavn = "Navny",
            mellomnavn = null,
            etternavn = "McNavnface",
            fortrolig = false,
            strengtFortrolig = false,
            strengtFortroligUtland = false,
        )
    }
}
