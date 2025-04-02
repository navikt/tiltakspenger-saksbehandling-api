package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import io.kotest.matchers.shouldBe
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class PersonhendelseServiceTest {
    @Test
    fun `behandlePersonhendelse - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val personhendelseRepository = testDataHelper.personhendelseRepository
            val sakPostgresRepo = testDataHelper.sakRepo
            val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository)
            val fnr = Fnr.random()

            personhendelseService.behandlePersonhendelse(
                getPersonhendelse(
                    fnr,
                    Doedsfall(LocalDate.now().minusDays(1)),
                    null,
                ),
            )

            personhendelseRepository.hent(fnr) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - dødsfall, finnes sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val personhendelseRepository = testDataHelper.personhendelseRepository
            val sakPostgresRepo = testDataHelper.sakRepo
            val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository)
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
            )
            val personhendelse = getPersonhendelse(fnr, Doedsfall(LocalDate.now().minusDays(1)), null)

            personhendelseService.behandlePersonhendelse(personhendelse)

            val personhendelser = personhendelseRepository.hent(fnr)
            personhendelser.size shouldBe 1
            val personhendelseDb = personhendelser.first()
            personhendelseDb.fnr shouldBe fnr
            personhendelseDb.hendelseId shouldBe personhendelse.hendelseId
            personhendelseDb.opplysningstype shouldBe Opplysningstype.DOEDSFALL_V1
            personhendelseDb.personhendelseType shouldBe PersonhendelseType.Doedsfall(LocalDate.now().minusDays(1))
            personhendelseDb.sakId shouldBe sak.id
            personhendelseDb.oppgaveId shouldBe null
            personhendelseDb.oppgaveSistSjekket shouldBe null
        }
    }

    @Test
    fun `behandlePersonhendelse - forelderbarnrelasjon, finnes sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val personhendelseRepository = testDataHelper.personhendelseRepository
            val sakPostgresRepo = testDataHelper.sakRepo
            val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository)
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
            )
            val personhendelse = getPersonhendelse(fnr, null, ForelderBarnRelasjon("12345678910", "BARN", "FAR"))

            personhendelseService.behandlePersonhendelse(personhendelse)

            val personhendelser = personhendelseRepository.hent(fnr)
            personhendelser.size shouldBe 1
            val personhendelseDb = personhendelser.first()
            personhendelseDb.fnr shouldBe fnr
            personhendelseDb.hendelseId shouldBe personhendelse.hendelseId
            personhendelseDb.opplysningstype shouldBe Opplysningstype.FORELDERBARNRELASJON_V1
            personhendelseDb.personhendelseType shouldBe PersonhendelseType.ForelderBarnRelasjon("12345678910", "FAR")
            personhendelseDb.sakId shouldBe sak.id
            personhendelseDb.oppgaveId shouldBe null
            personhendelseDb.oppgaveSistSjekket shouldBe null
        }
    }

    private fun getPersonhendelse(
        fnr: Fnr,
        doedsfall: Doedsfall?,
        forelderBarnRelasjon: ForelderBarnRelasjon?,
    ): Personhendelse {
        val personidenter = listOf("12345", fnr.verdi)
        val opplysningstype = if (doedsfall != null) {
            Opplysningstype.DOEDSFALL_V1.name
        } else {
            Opplysningstype.FORELDERBARNRELASJON_V1.name
        }
        return Personhendelse(
            "hendelseId",
            personidenter,
            "FREG",
            Instant.now(),
            opplysningstype,
            Endringstype.OPPRETTET,
            null,
            doedsfall,
            forelderBarnRelasjon,
        )
    }
}
