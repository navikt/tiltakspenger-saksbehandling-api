@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.atomic.Atomic
import io.github.serpro69.kfaker.faker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import java.time.Clock

class PersonFakeKlient(private val clock: Clock) : PersonKlient {
    private val data = Atomic(mutableMapOf<Fnr, EnkelPerson>())

    private val kall = Atomic(mutableListOf<Fnr>())
    val antallKall: Int get() = kall.get().size
    val alleKall: List<Fnr> get() = kall.get().toList()

    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson =
        data.get()[fnr] ?: personopplysningerSøkerFake(fnr)

    /**
     * Denne bør kalles av testoppsettet før vi lager en søknad.
     * Overskriver eksisterende personopplysninger for personen.
     */
    fun leggTilPersonopplysning(
        fnr: Fnr,
        personopplysninger: EnkelPerson,
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
            fødselsdato = 16.oktober(1995),
            mellomnavn = null,
            etternavn = faker.name.lastName(),
            fortrolig = fnr.verdi.startsWith('2'),
            strengtFortrolig = fnr.verdi.startsWith('3'),
            strengtFortroligUtland = fnr.verdi.startsWith('4'),
        )
    }

    private fun personopplysningerSøkerFake(fnr: Fnr): EnkelPerson {
        val person = enkelPersonFake(fnr)

        return EnkelPerson(
            fnr = fnr,
            fødselsdato = nå(clock).toLocalDate().minusYears(20),
            fornavn = person.fornavn,
            mellomnavn = person.mellomnavn,
            etternavn = person.etternavn,
            fortrolig = person.fortrolig,
            strengtFortrolig = person.strengtFortrolig,
            strengtFortroligUtland = person.strengtFortroligUtland,
        )
    }
}
