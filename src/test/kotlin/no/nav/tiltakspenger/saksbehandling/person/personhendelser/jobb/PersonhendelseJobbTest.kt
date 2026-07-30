package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PersonhendelseJobbTest {
    private val oppgaveId = OppgaveId("50")

    private fun nyOppgaveKlient(): OppgaveKlient = mockk<OppgaveKlient>().also {
        coEvery { it.opprettOppgaveUtenDuplikatkontroll(any(), any()) } returns oppgaveId.right()
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - ingen vedtak - sletter fra db`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val personhendelseDb = getPersonhendelseDb(
                    id = id,
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1,
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                personhendelseRepository.hent(sak.id) shouldBe emptyList()

                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - vedtak tilbake i tid - sletter fra db`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).minusWeeks(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                personhendelseRepository.hent(sak.id) shouldBe emptyList()

                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har vedtak nå - oppretter oppgave`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock = clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).plusWeeks(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 1
                val personhendelseFraDb = personhendelser.first()
                personhendelseFraDb.oppgaveId shouldBe oppgaveId

                coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.DOED) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har vedtak frem i tid - oppretter oppgave`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).plusDays(3)
                val deltakelsesTom = LocalDate.now(clock).plusMonths(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 1
                val personhendelseFraDb = personhendelser.first()
                personhendelseFraDb.oppgaveId shouldBe oppgaveId

                coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.DOED) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har vedtak nå, adressebeskyttelse - oppretter ikke oppgave`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).plusWeeks(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    opplysningstype = Opplysningstype.ADRESSEBESKYTTELSE_V1,
                    personhendelseType = PersonhendelseType.Adressebeskyttelse(Gradering.STRENGT_FORTROLIG.name),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                personhendelseRepository.hent(sak.id) shouldBe emptyList()

                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelser - har åpen behandling, adressebeskyttelse - oppretter oppgave`() {
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).plusWeeks(2)
                testDataHelper.persisterOpprettetSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    opplysningstype = Opplysningstype.ADRESSEBESKYTTELSE_V1,
                    personhendelseType = PersonhendelseType.Adressebeskyttelse(Gradering.STRENGT_FORTROLIG.name),
                    sakId = sak.id,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.opprettOppgaveForPersonhendelse(id)

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 1
                val personhendelseFraDb = personhendelser.first()
                personhendelseFraDb.oppgaveId shouldBe oppgaveId

                coVerify(exactly = 1) {
                    oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                        fnr,
                        Oppgavebehov.ADRESSEBESKYTTELSE,
                    )
                }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        val oppgaveKlient = nyOppgaveKlient()
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns false.right()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).plusDays(3)
                val deltakelsesTom = LocalDate.now(clock).plusMonths(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                    oppgaveId = oppgaveId,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.ryddOppPersonhendelse(id)

                val oppdatertPersonhendelseDb = personhendelseRepository.hent(sak.id).first()
                oppdatertPersonhendelseDb shouldNotBe null
                oppdatertPersonhendelseDb.oppgaveId shouldBe oppgaveId
                oppdatertPersonhendelseDb.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe nå(testDataHelper.clock)
                    .truncatedTo(ChronoUnit.MINUTES)
                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        val oppgaveKlient = nyOppgaveKlient()
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns true.right()
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).plusDays(3)
                val deltakelsesTom = LocalDate.now(clock).plusMonths(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                    sakId = sak.id,
                    oppgaveId = oppgaveId,
                )
                personhendelseRepository.lagre(personhendelseDb)

                personhendelseJobb.ryddOppPersonhendelse(id)

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `opprettOppgaveForPersonhendelser - jobben plukker kun opp hendelser uten oppgave`() {
        // TODO: Kan flippes til runIsolated = false med shouldContain/shouldNotContain på ID-spørringen og per-ID-kall i stedet for full jobbkjøring.
        val oppgaveKlient = nyOppgaveKlient()
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val id = UUID.randomUUID()
                val idMedOppgave = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).plusWeeks(2)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                personhendelseRepository.lagre(
                    getPersonhendelseDb(
                        id = id,
                        fnr = fnr,
                        opplysningstype = Opplysningstype.DOEDSFALL_V1,
                        personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                        sakId = sak.id,
                    ),
                )
                personhendelseRepository.lagre(
                    getPersonhendelseDb(
                        id = idMedOppgave,
                        fnr = fnr,
                        opplysningstype = Opplysningstype.DOEDSFALL_V1,
                        personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                        sakId = sak.id,
                        oppgaveId = OppgaveId("99"),
                    ),
                )

                personhendelseRepository.hentIderUtenOppgave() shouldBe listOf(id)

                personhendelseJobb.opprettOppgaveForPersonhendelser()

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.first { it.id == id }.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.DOED) }
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `opprydning - jobben plukker kun opp hendelser med oppgave som ikke nylig er sjekket`() {
        // TODO: Kan flippes til runIsolated = false med shouldContain/shouldNotContain på ID-spørringen og per-ID-kall i stedet for full jobbkjøring.
        val oppgaveKlient = nyOppgaveKlient()
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns false.right()
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val personhendelseJobb =
                    PersonhendelseJobb(personhendelseRepository, sakRepo, oppgaveKlient, clock)
                val idUtenOppgave = UUID.randomUUID()
                val idNyligSjekket = UUID.randomUUID()
                val idIkkeSjekket = UUID.randomUUID()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                personhendelseRepository.lagre(
                    getPersonhendelseDb(
                        id = idUtenOppgave,
                        fnr = fnr,
                        opplysningstype = Opplysningstype.DOEDSFALL_V1,
                        personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                        sakId = sak.id,
                    ),
                )
                personhendelseRepository.lagre(
                    getPersonhendelseDb(
                        id = idNyligSjekket,
                        fnr = fnr,
                        opplysningstype = Opplysningstype.DOEDSFALL_V1,
                        personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                        sakId = sak.id,
                        oppgaveId = OppgaveId("98"),
                        oppgaveSistSjekket = nå(clock),
                    ),
                )
                personhendelseRepository.lagre(
                    getPersonhendelseDb(
                        id = idIkkeSjekket,
                        fnr = fnr,
                        opplysningstype = Opplysningstype.DOEDSFALL_V1,
                        personhendelseType = PersonhendelseType.Doedsfall(LocalDate.now(clock)),
                        sakId = sak.id,
                        oppgaveId = oppgaveId,
                    ),
                )

                personhendelseRepository.hentIderMedOppgave() shouldBe listOf(idIkkeSjekket)

                personhendelseJobb.opprydning()

                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
                val oppdatert = personhendelseRepository.hent(sak.id).first { it.id == idIkkeSjekket }
                oppdatert.oppgaveSistSjekket shouldNotBe null
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
