package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import arrow.core.left
import arrow.core.right
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
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class PersonhendelseServiceTest {
    private val personKlient = mockk<PersonKlient>()

    @BeforeEach
    fun clearMockData() {
        clearMocks(personKlient)
    }

    @Test
    fun `behandlePersonhendelse - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
                val fnr = Fnr.random()

                personhendelseService.behandlePersonhendelse(
                    getPersonhendelse(
                        fnr = fnr,
                        doedsfall = Doedsfall(LocalDate.now(clock).minusDays(1)),
                        clock = clock,
                    ),
                ) shouldBe KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()

                personhendelseRepository.hentAlleUtenOppgave() shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - dødsfall, finnes sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    doedsfall = Doedsfall(LocalDate.now(clock).minusDays(1)),
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe Unit.right()

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 1
                val personhendelseDb = personhendelser.first()
                personhendelseDb.fnr shouldBe fnr
                personhendelseDb.hendelseId shouldBe personhendelse.hendelseId
                personhendelseDb.opplysningstype shouldBe Opplysningstype.DOEDSFALL_V1
                personhendelseDb.personhendelseType shouldBe PersonhendelseType.Doedsfall(
                    LocalDate.now(clock).minusDays(1),
                )
                personhendelseDb.sakId shouldBe sak.id
                personhendelseDb.oppgaveId shouldBe null
                personhendelseDb.oppgaveSistSjekket shouldBe null
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - forelderbarnrelasjon, skal ikke behandles`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    forelderBarnRelasjon = ForelderBarnRelasjon("12345678910", "BARN", "FAR"),
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 0
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, adressebeskyttet i PDL - oppdaterer og lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val (_, behandling, _) = testDataHelper.persisterOpprettetSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                testDataHelper.statistikkSakRepo.lagre(
                    behandling.genererSaksstatistikk(
                        hendelse = StatistikkhendelseType.OPPRETTET_BEHANDLING,
                    ).genererSaksstatistikk(
                        gjelderKode6 = { false },
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                    ),
                )
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                    clock = clock,
                )
                coEvery { personKlient.hentEnkelPerson(fnr) } returns EnkelPerson(
                    fnr = fnr,
                    fødselsdato = 16.oktober(1995),
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    fortrolig = false,
                    strengtFortrolig = true,
                    strengtFortroligUtland = false,
                    dødsdato = null,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe Unit.right()

                val personhendelser = personhendelseRepository.hent(sak.id)
                personhendelser.size shouldBe 1
                val personhendelseDb = personhendelser.first()
                personhendelseDb.fnr shouldBe fnr
                personhendelseDb.hendelseId shouldBe personhendelse.hendelseId
                personhendelseDb.opplysningstype shouldBe Opplysningstype.ADRESSEBESKYTTELSE_V1
                personhendelseDb.personhendelseType shouldBe PersonhendelseType.Adressebeskyttelse(
                    "STRENGT_FORTROLIG",
                )
                personhendelseDb.sakId shouldBe sak.id
                personhendelseDb.oppgaveId shouldBe null
                personhendelseDb.oppgaveSistSjekket shouldBe null

                val statistikkSakDTO = testDataHelper.statistikkSakRepo.hent(sak.id).first()
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
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkSakRepo = testDataHelper.statistikkSakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val (_, behandling, _) = testDataHelper.persisterOpprettetSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                statistikkSakRepo.lagre(
                    behandling.genererSaksstatistikk(
                        hendelse = StatistikkhendelseType.OPPRETTET_BEHANDLING,
                    ).genererSaksstatistikk(
                        versjon = "1",
                        clock = Clock.system(zoneIdOslo),
                        gjelderKode6 = { false },
                    ),
                )
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                    clock = clock,
                )
                coEvery { personKlient.hentEnkelPerson(fnr) } returns EnkelPerson(
                    fnr = fnr,

                    fødselsdato = 16.oktober(1995),
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    fortrolig = false,
                    strengtFortrolig = false,
                    strengtFortroligUtland = false,
                    dødsdato = null,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.IkkeKode6IPdl.left()

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
                val statistikkSakDTO = statistikkSakRepo.hent(sak.id).first()
                statistikkSakDTO.fnr shouldBe fnr.verdi
                statistikkSakDTO.opprettetAv shouldNotBe "-5"
                statistikkSakDTO.saksbehandler shouldNotBe "-5"
                statistikkSakDTO.ansvarligBeslutter shouldNotBe "-5"
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - ukjent opplysningstype, finnes sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    opplysningstype = "NAVN_V1",
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - DOEDSFALL_V1 men doedsfall-felt er null - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                // DOEDSFALL_V1 uten doedsfall-payload — fanget av defensiv guard i servicen.
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1.name,
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.PayloadMangler.left()

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse med gradering FORTROLIG - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                // Vi bryr oss kun om STRENGT_FORTROLIG[_UTLAND] (kode 6). FORTROLIG (kode 7) skal ignoreres.
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.FORTROLIG),
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.AdressebeskyttelseErIkkeKode6.left()

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - ingen av personidentene matcher en sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
                // Sak finnes, men ikke for fnr-et i hendelsen.
                val sakFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = sakFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = sakFnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = sakFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val ukjentFnr = Fnr.random()
                val personhendelse = getPersonhendelse(
                    fnr = ukjentFnr,
                    doedsfall = Doedsfall(LocalDate.now(clock).minusDays(1)),
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()

                personhendelseRepository.hent(sak.id) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - samme hendelse mottatt to ganger - lagrer kun første`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val personhendelseRepository = testDataHelper.personhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val statistikkService = testDataHelper.statistikkService
                val personhendelseService =
                    PersonhendelseService(sakPostgresRepo, personhendelseRepository, personKlient, statistikkService)
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
                val personhendelse = getPersonhendelse(
                    fnr = fnr,
                    doedsfall = Doedsfall(LocalDate.now(clock).minusDays(1)),
                    clock = clock,
                )

                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe Unit.right()
                personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                    KunneIkkeBehandlePersonhendelse.HendelseAlleredeLagret.left()

                personhendelseRepository.hent(sak.id).size shouldBe 1
            }
        }
    }

    private fun getPersonhendelse(
        fnr: Fnr,
        doedsfall: Doedsfall? = null,
        forelderBarnRelasjon: ForelderBarnRelasjon? = null,
        adressebeskyttelse: Adressebeskyttelse? = null,
        opplysningstype: String? = null,
        clock: Clock,
    ): Personhendelse {
        val personidenter = listOf("12345", fnr.verdi)

        val resolvedOpplysningstype = opplysningstype ?: when {
            doedsfall != null -> Opplysningstype.DOEDSFALL_V1.name
            forelderBarnRelasjon != null -> "FORELDERBARNRELASJON_V1"
            else -> Opplysningstype.ADRESSEBESKYTTELSE_V1.name
        }

        return Personhendelse(
            "hendelseId",
            personidenter,
            "FREG",
            Instant.now(clock),
            resolvedOpplysningstype,
            Endringstype.OPPRETTET,
            null,
            doedsfall,
            forelderBarnRelasjon,
            adressebeskyttelse,
        )
    }
}
