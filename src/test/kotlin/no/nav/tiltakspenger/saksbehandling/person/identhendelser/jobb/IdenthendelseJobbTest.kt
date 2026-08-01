package no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.infra.Producer
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Identtype
import no.nav.tiltakspenger.saksbehandling.person.Personident
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseDto
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseDb
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.lagreSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.lagreStønadsstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.genererStønadsstatistikkForRammevedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class IdenthendelseJobbTest {
    private fun nyKafkaProducer(): Producer<String, String> = mockk<Producer<String, String>>().also {
        coEvery { it.produce(any(), any(), any()) } just Runs
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er ikke behandlet - oppdaterer i database og produserer til kafka`() {
        val kafkaProducer = nyKafkaProducer()
        val identhendelseKafkaProducer = IdenthendelseKafkaProducer(kafkaProducer, "topic")
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkService = testDataHelper.statistikkService
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkService = statistikkService,
                    sessionFactory = testDataHelper.sessionFactory,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()

                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).minusWeeks(2)
                val (_, vedtak, _) = testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = gammeltFnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                testDataHelper.sessionFactory.lagreSaksstatistikk(
                    vedtak.genererSaksstatistikk().genererSaksstatistikk(
                        gjelderKode6 = { false },
                        versjon = "1",
                        clock = clock,
                    ),
                )
                testDataHelper.sessionFactory.lagreStønadsstatistikk(
                    genererStønadsstatistikkForRammevedtak(vedtak).genererStønadsstatistikk(),
                    clock,
                )
                val identhendelseDb = IdenthendelseDb(
                    id = UUID.randomUUID(),
                    gammeltFnr = gammeltFnr,
                    nyttFnr = nyttFnr,
                    sakId = sak.id,
                    personidenter = listOf(
                        Personident(nyttFnr.verdi, false, Identtype.FOLKEREGISTERIDENT),
                        Personident(gammeltFnr.verdi, true, Identtype.FOLKEREGISTERIDENT),
                    ),
                    produsertHendelse = null,
                    oppdatertDatabase = null,
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelse(identhendelseDb.id)

                coVerify(exactly = 1) {
                    kafkaProducer.produce(
                        any(),
                        identhendelseDb.id.toString(),
                        objectMapper.writeValueAsString(IdenthendelseDto(gammeltFnr.verdi, nyttFnr.verdi)),
                    )
                }
                val oppdatertIdenthendelseDb = identhendelseRepository.hent(identhendelseDb.id)
                oppdatertIdenthendelseDb shouldNotBe null
                oppdatertIdenthendelseDb?.produsertHendelse?.toLocalDate() shouldBe 1.januar(2025)
                oppdatertIdenthendelseDb?.oppdatertDatabase?.toLocalDate() shouldBe 1.januar(2025)

                sakRepo.hentForSakId(sak.id)?.fnr shouldBe nyttFnr
                søknadRepo.hentSøknaderForFnr(gammeltFnr) shouldBe emptyList()
                søknadRepo.hentSøknaderForFnr(nyttFnr).size shouldBe 1
                testDataHelper.sessionFactory.hentSaksstatistikk(sak.id).first().fnr shouldBe nyttFnr.verdi
                testDataHelper.sessionFactory.hentSaksstatistikk(sak.id).first().fnr shouldBe nyttFnr.verdi
            }
        }
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er produsert på kafka, ikke oppdatert i db - oppdaterer i database`() {
        val kafkaProducer = nyKafkaProducer()
        val identhendelseKafkaProducer = IdenthendelseKafkaProducer(kafkaProducer, "topic")
        withMigratedDb { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkService = testDataHelper.statistikkService
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkService = statistikkService,
                    sessionFactory = testDataHelper.sessionFactory,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()

                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                val deltakelseFom = LocalDate.now(clock).minusMonths(3)
                val deltakelsesTom = LocalDate.now(clock).minusWeeks(2)
                val (_, vedtak, _) = testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = gammeltFnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                testDataHelper.sessionFactory.lagreSaksstatistikk(
                    vedtak.genererSaksstatistikk().genererSaksstatistikk(
                        gjelderKode6 = { false },
                        versjon = "1",
                        clock = clock,
                    ),
                )
                testDataHelper.sessionFactory.lagreStønadsstatistikk(
                    genererStønadsstatistikkForRammevedtak(vedtak).genererStønadsstatistikk(),
                    clock,
                )
                val identhendelseDb = IdenthendelseDb(
                    id = UUID.randomUUID(),
                    gammeltFnr = gammeltFnr,
                    nyttFnr = nyttFnr,
                    sakId = sak.id,
                    personidenter = listOf(
                        Personident(nyttFnr.verdi, false, Identtype.FOLKEREGISTERIDENT),
                        Personident(gammeltFnr.verdi, true, Identtype.FOLKEREGISTERIDENT),
                    ),
                    produsertHendelse = nå(testDataHelper.clock),
                    oppdatertDatabase = null,
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelse(identhendelseDb.id)

                coVerify(exactly = 0) { kafkaProducer.produce(any(), any(), any()) }

                val oppdatertIdenthendelseDb = identhendelseRepository.hent(identhendelseDb.id)
                oppdatertIdenthendelseDb shouldNotBe null
                oppdatertIdenthendelseDb?.produsertHendelse?.toLocalDate() shouldBe 1.januar(2025)
                oppdatertIdenthendelseDb?.oppdatertDatabase?.toLocalDate() shouldBe 1.januar(2025)

                sakRepo.hentForSakId(sak.id)?.fnr shouldBe nyttFnr
                søknadRepo.hentSøknaderForFnr(gammeltFnr) shouldBe emptyList()
                søknadRepo.hentSøknaderForFnr(nyttFnr).size shouldBe 1
                testDataHelper.sessionFactory.hentSaksstatistikk(sak.id).first().fnr shouldBe nyttFnr.verdi
                testDataHelper.sessionFactory.hentSaksstatistikk(sak.id).first().fnr shouldBe nyttFnr.verdi
            }
        }
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er ferdig behandlet - ignorerer`() {
        val kafkaProducer = nyKafkaProducer()
        val identhendelseKafkaProducer = IdenthendelseKafkaProducer(kafkaProducer, "topic")
        withMigratedDb { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkService = testDataHelper.statistikkService
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkService = statistikkService,
                    sessionFactory = testDataHelper.sessionFactory,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = nyttFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = nyttFnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = nyttFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val identhendelseDb = IdenthendelseDb(
                    id = UUID.randomUUID(),
                    gammeltFnr = gammeltFnr,
                    nyttFnr = nyttFnr,
                    sakId = sak.id,
                    personidenter = listOf(
                        Personident(nyttFnr.verdi, false, Identtype.FOLKEREGISTERIDENT),
                        Personident(gammeltFnr.verdi, true, Identtype.FOLKEREGISTERIDENT),
                    ),
                    produsertHendelse = nå(testDataHelper.clock),
                    oppdatertDatabase = nå(testDataHelper.clock),
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelse(identhendelseDb.id)

                coVerify(exactly = 0) { kafkaProducer.produce(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `behandleIdenthendelser - jobben plukker kun opp hendelser som ikke er ferdig behandlet`() {
        val kafkaProducer = nyKafkaProducer()
        val identhendelseKafkaProducer = IdenthendelseKafkaProducer(kafkaProducer, "topic")
        withMigratedDb { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = testDataHelper.sakRepo,
                    søknadRepo = testDataHelper.søknadRepo,
                    statistikkService = testDataHelper.statistikkService,
                    sessionFactory = testDataHelper.sessionFactory,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = gammeltFnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val ubehandlet = IdenthendelseDb(
                    id = UUID.randomUUID(),
                    gammeltFnr = gammeltFnr,
                    nyttFnr = nyttFnr,
                    sakId = sak.id,
                    personidenter = listOf(
                        Personident(nyttFnr.verdi, false, Identtype.FOLKEREGISTERIDENT),
                        Personident(gammeltFnr.verdi, true, Identtype.FOLKEREGISTERIDENT),
                    ),
                    produsertHendelse = null,
                    oppdatertDatabase = null,
                )
                val ferdigBehandlet = ubehandlet.copy(
                    id = UUID.randomUUID(),
                    produsertHendelse = nå(testDataHelper.clock),
                    oppdatertDatabase = nå(testDataHelper.clock),
                )
                identhendelseRepository.lagre(ubehandlet)
                identhendelseRepository.lagre(ferdigBehandlet)

                val iderSomIkkeErBehandlet = identhendelseRepository.hentIderSomIkkeErBehandlet()
                iderSomIkkeErBehandlet shouldContain ubehandlet.id
                iderSomIkkeErBehandlet shouldNotContain ferdigBehandlet.id

                identhendelseJobb.behandleIdenthendelse(ubehandlet.id)

                val oppdatert = identhendelseRepository.hent(ubehandlet.id)
                oppdatert?.produsertHendelse shouldNotBe null
                oppdatert?.oppdatertDatabase shouldNotBe null
                coVerify(exactly = 1) { kafkaProducer.produce(any(), ubehandlet.id.toString(), any()) }
            }
        }
    }
}
