package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonGateway
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class PersonhendelseServiceTest {
    private val personGateway = mockk<PersonGateway>()

    @BeforeEach
    fun clearMockData() {
        clearMocks(personGateway)
    }

    @Test
    fun `behandlePersonhendelse - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
                val fnr = Fnr.random()

                personhendelseService.behandlePersonhendelse(
                    getPersonhendelse(
                        fnr = fnr,
                        doedsfall = Doedsfall(LocalDate.now().minusDays(1)),
                    ),
                )

                personhendelseRepository.hent(fnr) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - dødsfall, finnes sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    doedsfall = Doedsfall(LocalDate.now().minusDays(1)),
                )

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
    }

    @Test
    fun `behandlePersonhendelse - forelderbarnrelasjon, bruker er forelder, finnes sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    forelderBarnRelasjon = ForelderBarnRelasjon("12345678910", "BARN", "FAR"),
                )

                personhendelseService.behandlePersonhendelse(personhendelse)

                val personhendelser = personhendelseRepository.hent(fnr)
                personhendelser.size shouldBe 1
                val personhendelseDb = personhendelser.first()
                personhendelseDb.fnr shouldBe fnr
                personhendelseDb.hendelseId shouldBe personhendelse.hendelseId
                personhendelseDb.opplysningstype shouldBe Opplysningstype.FORELDERBARNRELASJON_V1
                personhendelseDb.personhendelseType shouldBe PersonhendelseType.ForelderBarnRelasjon(
                    "12345678910",
                    "FAR",
                )
                personhendelseDb.sakId shouldBe sak.id
                personhendelseDb.oppgaveId shouldBe null
                personhendelseDb.oppgaveSistSjekket shouldBe null
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - forelderbarnrelasjon, bruker er barn, finnes sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    forelderBarnRelasjon = ForelderBarnRelasjon("12345678910", "FAR", "BARN"),
                )

                personhendelseService.behandlePersonhendelse(personhendelse)

                personhendelseRepository.hent(fnr) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, adressebeskyttet i PDL - oppdaterer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val (_, behandling, _) = testDataHelper.persisterOpprettetFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                statistikkSakRepo.lagre(
                    genererStatistikkForNyFørstegangsbehandling(
                        behandling = behandling,
                        gjelderKode6 = false,
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                    ),
                )
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                )
                coEvery { personGateway.hentEnkelPerson(fnr) } returns EnkelPerson(
                    fnr = fnr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    fortrolig = false,
                    strengtFortrolig = true,
                    strengtFortroligUtland = false,
                )

                personhendelseService.behandlePersonhendelse(personhendelse)

                personhendelseRepository.hent(fnr) shouldBe emptyList()
                val statistikkSakDTO = statistikkSakRepo.hent(sak.id).first()
                statistikkSakDTO.fnr shouldBe fnr.verdi
                statistikkSakDTO.opprettetAv shouldBe "-5"
                statistikkSakDTO.saksbehandler shouldBe "-5"
                statistikkSakDTO.ansvarligBeslutter shouldBe "-5"
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, ikke adressebeskyttet i PDL - oppdaterer ikke`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val personhendelseService = PersonhendelseService(sakPostgresRepo, personhendelseRepository, personGateway, statistikkSakRepo)
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val (_, behandling, _) = testDataHelper.persisterOpprettetFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                statistikkSakRepo.lagre(
                    genererStatistikkForNyFørstegangsbehandling(
                        behandling = behandling,
                        gjelderKode6 = false,
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                    ),
                )
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                )
                coEvery { personGateway.hentEnkelPerson(fnr) } returns EnkelPerson(
                    fnr = fnr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    fortrolig = false,
                    strengtFortrolig = false,
                    strengtFortroligUtland = false,
                )

                personhendelseService.behandlePersonhendelse(personhendelse)

                personhendelseRepository.hent(fnr) shouldBe emptyList()
                val statistikkSakDTO = statistikkSakRepo.hent(sak.id).first()
                statistikkSakDTO.fnr shouldBe fnr.verdi
                statistikkSakDTO.opprettetAv shouldNotBe "-5"
                statistikkSakDTO.saksbehandler shouldNotBe "-5"
                statistikkSakDTO.ansvarligBeslutter shouldNotBe "-5"
            }
        }
    }

    private fun getPersonhendelse(
        fnr: Fnr,
        doedsfall: Doedsfall? = null,
        forelderBarnRelasjon: ForelderBarnRelasjon? = null,
        adressebeskyttelse: Adressebeskyttelse? = null,
    ): Personhendelse {
        val personidenter = listOf("12345", fnr.verdi)

        val opplysningstype = if (doedsfall != null) {
            Opplysningstype.DOEDSFALL_V1.name
        } else if (forelderBarnRelasjon != null) {
            Opplysningstype.FORELDERBARNRELASJON_V1.name
        } else {
            Opplysningstype.ADRESSEBESKYTTELSE_V1.name
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
            adressebeskyttelse,
        )
    }
}
