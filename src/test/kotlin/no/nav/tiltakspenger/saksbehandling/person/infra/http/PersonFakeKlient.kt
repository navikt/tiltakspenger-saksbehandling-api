@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.atomic.Atomic
import io.github.serpro69.kfaker.faker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.personklient.pdl.dto.EndringsMetadata
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjonRolle
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.Personident
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class PersonFakeKlient(private val clock: Clock) : PersonKlient {
    private val data = Atomic(mutableMapOf<Fnr, EnkelPerson>())

    private val kall = Atomic(mutableListOf<Fnr>())
    val antallKall: Int get() = kall.get().size
    val alleKall: List<Fnr> get() = kall.get().toList()

    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson =
        data.get()[fnr] ?: personopplysningerSøkerFake(fnr)

    override suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon> {
        return forelderBarnRelasjonFake()
    }

    override suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson> =
        fnrs.map { fnr -> hentEnkelPerson(fnr) }

    override suspend fun hentIdenter(aktorId: String): List<Personident> {
        return emptyList()
    }

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
            fødselsdato = tilfeldigFødselsdato0Til16År(clock, fnr),
            mellomnavn = null,
            etternavn = faker.name.lastName(),
            fortrolig = fnr.verdi.startsWith('2'),
            strengtFortrolig = fnr.verdi.startsWith('3'),
            strengtFortroligUtland = fnr.verdi.startsWith('4'),
            dødsdato = null,
        )
    }

    private fun forelderBarnRelasjonFake(): List<ForelderBarnRelasjon> {
        return listOf(
            ForelderBarnRelasjon(
                relatertPersonsIdent = Fnr.random().verdi.replaceFirstChar { "1" }, // Ingen adressebeskyttelse, basert på [enkelPersonFake]
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.MOR,
                relatertPersonUtenFolkeregisteridentifikator = null,
                folkeregistermetadata = null,
                metadata = EndringsMetadata(emptyList(), "Jungeltelegrafen"),
            ),
            ForelderBarnRelasjon(
                relatertPersonsIdent = Fnr.random().verdi.replaceFirstChar { "2" }, // Fortrolig, basert på [enkelPersonFake]
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.MOR,
                relatertPersonUtenFolkeregisteridentifikator = null,
                folkeregistermetadata = null,
                metadata = EndringsMetadata(emptyList(), "Jungeltelegrafen"),
            ),
            ForelderBarnRelasjon(
                relatertPersonsIdent = Fnr.random().verdi.replaceFirstChar { "3" }, // Strengt fortrolig, basert på [enkelPersonFake]
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.MOR,
                relatertPersonUtenFolkeregisteridentifikator = null,
                folkeregistermetadata = null,
                metadata = EndringsMetadata(emptyList(), "Jungeltelegrafen"),
            ),
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
            dødsdato = null,
        )
    }

    private fun tilfeldigFødselsdato0Til16År(clock: Clock, seed: Fnr): LocalDate {
        val slutt = LocalDate.now(clock)
        val start = slutt.minusYears(16)
        val dager = ChronoUnit.DAYS.between(start, slutt)
        val rnd = Random(seed.verdi.toLong())
        val offset = rnd.nextLong(dager + 1)
        return start.plusDays(offset)
    }
}
