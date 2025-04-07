package no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.genererSaksstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.Identtype
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.Personident
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseDto
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class IdenthendelseJobbTest {
    private val kafkaProducer = mockk<Producer<String, String>>()
    private val identhendelseKafkaProducer = IdenthendelseKafkaProducer(kafkaProducer, "topic")

    @BeforeEach
    fun clearMockData() {
        clearMocks(kafkaProducer)
        coEvery {
            kafkaProducer.produce(
                any(),
                any(),
                any(),
            )
        } just Runs
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er ikke behandlet - oppdaterer i database og produserer til kafka`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val statistikkStønadRepo = testDataHelper.statistikkStønadRepo
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkSakRepo = statistikkSakRepo,
                    statistikkStønadRepo = statistikkStønadRepo,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()

                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                val deltakelseFom = LocalDate.now().minusMonths(3)
                val deltakelsesTom = LocalDate.now().minusWeeks(2)
                val (_, vedtak, _) = testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = gammeltFnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                statistikkSakRepo.lagre(
                    genererSaksstatistikkForRammevedtak(
                        vedtak = vedtak,
                        gjelderKode6 = false,
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                    ),
                )
                statistikkStønadRepo.lagre(
                    genererStønadsstatistikkForRammevedtak(
                        vedtak,
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
                    produsertHendelse = null,
                    oppdatertDatabase = null,
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelser()

                coVerify(exactly = 1) {
                    kafkaProducer.produce(
                        any(),
                        identhendelseDb.id.toString(),
                        objectMapper.writeValueAsString(IdenthendelseDto(gammeltFnr.verdi, nyttFnr.verdi)),
                    )
                }
                val oppdatertIdenthendelseDb = identhendelseRepository.hent(identhendelseDb.id)
                oppdatertIdenthendelseDb shouldNotBe null
                oppdatertIdenthendelseDb?.produsertHendelse?.toLocalDate() shouldBe LocalDate.now()
                oppdatertIdenthendelseDb?.oppdatertDatabase?.toLocalDate() shouldBe LocalDate.now()

                sakRepo.hentForSakId(sak.id)?.fnr shouldBe nyttFnr
                søknadRepo.hentSøknaderForFnr(gammeltFnr) shouldBe emptyList()
                søknadRepo.hentSøknaderForFnr(nyttFnr).size shouldBe 1
                statistikkSakRepo.hent(sak.id).first().fnr shouldBe nyttFnr.verdi
                statistikkSakRepo.hent(sak.id).first().fnr shouldBe nyttFnr.verdi
            }
        }
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er produsert på kafka, ikke oppdatert i db - oppdaterer i database`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val statistikkStønadRepo = testDataHelper.statistikkStønadRepo
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkSakRepo = statistikkSakRepo,
                    statistikkStønadRepo = statistikkStønadRepo,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()

                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                val deltakelseFom = LocalDate.now().minusMonths(3)
                val deltakelsesTom = LocalDate.now().minusWeeks(2)
                val (_, vedtak, _) = testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = gammeltFnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                statistikkSakRepo.lagre(
                    genererSaksstatistikkForRammevedtak(
                        vedtak = vedtak,
                        gjelderKode6 = false,
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                    ),
                )
                statistikkStønadRepo.lagre(
                    genererStønadsstatistikkForRammevedtak(
                        vedtak,
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
                    produsertHendelse = LocalDateTime.now(),
                    oppdatertDatabase = null,
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelser()

                coVerify(exactly = 0) { kafkaProducer.produce(any(), any(), any()) }

                val oppdatertIdenthendelseDb = identhendelseRepository.hent(identhendelseDb.id)
                oppdatertIdenthendelseDb shouldNotBe null
                oppdatertIdenthendelseDb?.produsertHendelse?.toLocalDate() shouldBe LocalDate.now()
                oppdatertIdenthendelseDb?.oppdatertDatabase?.toLocalDate() shouldBe LocalDate.now()

                sakRepo.hentForSakId(sak.id)?.fnr shouldBe nyttFnr
                søknadRepo.hentSøknaderForFnr(gammeltFnr) shouldBe emptyList()
                søknadRepo.hentSøknaderForFnr(nyttFnr).size shouldBe 1
                statistikkSakRepo.hent(sak.id).first().fnr shouldBe nyttFnr.verdi
                statistikkSakRepo.hent(sak.id).first().fnr shouldBe nyttFnr.verdi
            }
        }
    }

    @Test
    fun `behandleIdenthendelser - hendelsen er ferdig behandlet - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakRepo = testDataHelper.sakRepo
                val søknadRepo = testDataHelper.søknadRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val statistikkStønadRepo = testDataHelper.statistikkStønadRepo
                val identhendelseJobb = IdenthendelseJobb(
                    identhendelseRepository = identhendelseRepository,
                    identhendelseKafkaProducer = identhendelseKafkaProducer,
                    sakRepo = sakRepo,
                    søknadRepo = søknadRepo,
                    statistikkSakRepo = statistikkSakRepo,
                    statistikkStønadRepo = statistikkStønadRepo,
                )
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = nyttFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = nyttFnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
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
                    produsertHendelse = LocalDateTime.now(),
                    oppdatertDatabase = LocalDateTime.now(),
                )
                identhendelseRepository.lagre(identhendelseDb)

                identhendelseJobb.behandleIdenthendelser()

                coVerify(exactly = 0) { kafkaProducer.produce(any(), any(), any()) }
            }
        }
    }
}
