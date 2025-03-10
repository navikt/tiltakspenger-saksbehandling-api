package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import io.github.serpro69.kfaker.faker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger.EnkelPerson
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.PersonGateway

class PersonFakeGateway : PersonGateway {
    private val data = Atomic(mutableMapOf<Fnr, PersonopplysningerSøker>())

    private val kall = Atomic(mutableListOf<Fnr>())
    val antallKall: Int get() = kall.get().size
    val alleKall: List<Fnr> get() = kall.get().toList()

    override suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker =
        data.get()[fnr] ?: personopplysningerSøkerFake(fnr)

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
    } ?: enkelPersonFake(fnr)

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

    private fun enkelPersonFake(fnr: Fnr): EnkelPerson {
        val faker = faker {
            fakerConfig {
                randomSeed = fnr.verdi.toLong()
                locale = "nb-NO"
            }
        }

        return EnkelPerson(
            fnr = fnr,
            fornavn = faker.name.firstName(),
            mellomnavn = null,
            etternavn = faker.name.lastName(),
            fortrolig = fnr.verdi.startsWith('2'),
            strengtFortrolig = fnr.verdi.startsWith('3'),
            strengtFortroligUtland = fnr.verdi.startsWith('4'),
        )
    }

    private fun personopplysningerSøkerFake(fnr: Fnr): PersonopplysningerSøker {
        val person = enkelPersonFake(fnr)

        return PersonopplysningerSøker(
            fnr = fnr,
            fødselsdato = nå().toLocalDate().minusYears(20),
            fornavn = person.fornavn,
            mellomnavn = person.mellomnavn,
            etternavn = person.etternavn,
            fortrolig = person.fortrolig,
            strengtFortrolig = person.strengtFortrolig,
            strengtFortroligUtland = person.strengtFortroligUtland,
        )
    }
}
