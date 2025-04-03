package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PersonhendelseJobbTest {
    private val oppgaveGateway = mockk<OppgaveGateway>()
    private val oppgaveId = OppgaveId("50")

    @BeforeEach
    fun clearMockData() {
        clearMocks(oppgaveGateway)
        coEvery {
            oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(
                any(),
                any(),
            )
        } returns oppgaveId
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - ingen vedtak - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
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
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1,
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now()),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelser()

                personhendelseRepository.hent(fnr) shouldBe emptyList()

                coVerify(exactly = 0) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - vedtak tilbake i tid - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now().minusMonths(3)
                val deltakelsesTom = LocalDate.now().minusWeeks(2)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1,
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now()),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelser()

                personhendelseRepository.hent(fnr) shouldBe emptyList()

                coVerify(exactly = 0) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har vedtak nå - oppretter oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now().minusMonths(3)
                val deltakelsesTom = LocalDate.now().plusWeeks(2)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1,
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now()),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelser()

                val personhendelser = personhendelseRepository.hent(fnr)
                personhendelser.size shouldBe 1
                val personhendelseFraDb = personhendelser.first()
                personhendelseFraDb.oppgaveId shouldBe oppgaveId

                coVerify(exactly = 1) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.DOED) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har vedtak frem i tid - oppretter oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now().plusDays(3)
                val deltakelsesTom = LocalDate.now().plusMonths(2)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.FORELDERBARNRELASJON_V1,
                    personhendelseType = PersonhendelseType.ForelderBarnRelasjon("12345678910", "MOR"),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelser()

                val personhendelser = personhendelseRepository.hent(fnr)
                personhendelser.size shouldBe 1
                val personhendelseFraDb = personhendelser.first()
                personhendelseFraDb.oppgaveId shouldBe oppgaveId

                coVerify(exactly = 1) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.FATT_BARN) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        coEvery { oppgaveGateway.erFerdigstilt(any()) } returns false
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now().plusDays(3)
                val deltakelsesTom = LocalDate.now().plusMonths(2)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.FORELDERBARNRELASJON_V1,
                    personhendelseType = PersonhendelseType.ForelderBarnRelasjon("12345678910", "MOR"),
                    sakId = sak.id,
                    oppgaveId = oppgaveId,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprydning()

                val oppdatertPersonhendelseDb = personhendelseRepository.hent(fnr).first()
                oppdatertPersonhendelseDb shouldNotBe null
                oppdatertPersonhendelseDb.oppgaveId shouldBe oppgaveId
                oppdatertPersonhendelseDb.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe LocalDateTime.now()
                    .truncatedTo(ChronoUnit.MINUTES)
                coVerify(exactly = 1) { oppgaveGateway.erFerdigstilt(oppgaveId) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        coEvery { oppgaveGateway.erFerdigstilt(any()) } returns true
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now().plusDays(3)
                val deltakelsesTom = LocalDate.now().plusMonths(2)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.FORELDERBARNRELASJON_V1,
                    personhendelseType = PersonhendelseType.ForelderBarnRelasjon("12345678910", "MOR"),
                    sakId = sak.id,
                    oppgaveId = oppgaveId,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprydning()

                personhendelseRepository.hent(fnr) shouldBe emptyList()
                coVerify(exactly = 1) { oppgaveGateway.erFerdigstilt(oppgaveId) }
            }
        }
    }

    private fun getPersonhendelseDb(
        id: UUID = UUID.randomUUID(),
        fnr: Fnr,
        hendelseId: String = UUID.randomUUID().toString(),
        opplysningstype: Opplysningstype,
        personhendelseType: PersonhendelseType,
        sakId: SakId = SakId.random(),
        oppgaveId: OppgaveId? = null,
        oppgaveSistSjekket: LocalDateTime? = null,
    ) =
        PersonhendelseDb(
            id = id,
            fnr = fnr,
            hendelseId = hendelseId,
            opplysningstype = opplysningstype,
            personhendelseType = personhendelseType,
            sakId = sakId,
            oppgaveId = oppgaveId,
            oppgaveSistSjekket = oppgaveSistSjekket,
        )
}
