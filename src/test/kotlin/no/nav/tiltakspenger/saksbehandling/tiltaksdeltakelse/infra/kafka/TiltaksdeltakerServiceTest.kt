package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.DeltakerV1Dto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak.AvtaleDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class TiltaksdeltakerServiceTest {
    private val arenaDeltakerMapper = ArenaDeltakerMapper()

    @Test
    fun `behandleMottattArenadeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val deltakerId = "123456789"

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingString())

            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = id),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingString())

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(id)
            hendelser shouldHaveSize 1
            val tiltaksdeltakerHendelse = hendelser.first()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerHendelse.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, har eksternId - oppdaterer eksternId og lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val nyEksternId = UUID.fromString("9bedf708-1aa2-4be0-a561-cbe60ff2e9f9")
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknadstiltak = ObjectMother.søknadstiltak(id = id)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = soknadstiltak,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingMedEksternIdString())

            tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldBe null
            val oppdatertTiltaksdeltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString())
            oppdatertTiltaksdeltaker?.id shouldBe soknadstiltak.tiltaksdeltakerId
            oppdatertTiltaksdeltaker?.eksternId shouldBe nyEksternId.toString()
            oppdatertTiltaksdeltaker?.tiltakstype shouldBe soknadstiltak.typeKode
            oppdatertTiltaksdeltaker?.utdatertEksternId shouldBe id

            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(id).shouldBeEmpty()
            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(nyEksternId.toString())
            hendelser shouldHaveSize 1
            val tiltaksdeltakerHendelse = hendelser.first()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerHendelse.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak for arena-eksternId - lagrer ikke`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val nyEksternId = UUID.fromString("9bedf708-1aa2-4be0-a561-cbe60ff2e9f9")
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknadstiltak = ObjectMother.søknadstiltak(id = nyEksternId.toString())
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = soknadstiltak,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingMedEksternIdString())

            tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString()) shouldNotBe null
            tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldBe null

            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(nyEksternId.toString()).shouldBeEmpty()
            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(id).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = id),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )
            val opprinneligTiltaksdeltakerHendelse = TiltaksdeltakerHendelse(
                id = TiltaksdeltakerHendelseId.random(),
                eksternDeltakerId = id,
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                internDeltakerId = soknad.tiltak.tiltaksdeltakerId,
                behandlingId = null,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerHendelse, "melding", TiltaksdeltakerHendelseKilde.Arena)

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingString())

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(id)
            hendelser shouldHaveSize 2
            val opprinnelig = tiltaksdeltakerKafkaRepository.hent(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.first { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            nyHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            nyHendelse.dagerPerUke shouldBe 2.0F
            nyHendelse.deltakelsesprosent shouldBe 50.0F
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id

            tiltaksdeltakerService.behandleMottattKometdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(kometDeltaker),
            )

            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId.toString()).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId.toString()),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )

            tiltaksdeltakerService.behandleMottattKometdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(kometDeltaker),
            )

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId.toString())
            hendelser shouldHaveSize 1
            val tiltaksdeltakerHendelse = hendelser.first()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            tiltaksdeltakerHendelse.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId.toString()),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )
            val opprinneligTiltaksdeltakerHendelse = TiltaksdeltakerHendelse(
                id = TiltaksdeltakerHendelseId.random(),
                eksternDeltakerId = deltakerId.toString(),
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                internDeltakerId = soknad.tiltak.tiltaksdeltakerId,
                behandlingId = null,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerHendelse, "melding", TiltaksdeltakerHendelseKilde.Komet)

            tiltaksdeltakerService.behandleMottattKometdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(kometDeltaker),
            )

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId.toString())
            hendelser shouldHaveSize 2
            val opprinnelig = tiltaksdeltakerKafkaRepository.hent(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.first { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            nyHendelse.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            nyHendelse.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            nyHendelse.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()

            tiltaksdeltakerService.behandleMottattTeamTiltakdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(teamTiltakDeltaker),
            )

            tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )

            tiltaksdeltakerService.behandleMottattTeamTiltakdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(teamTiltakDeltaker),
            )

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId)
            hendelser shouldHaveSize 1
            val tiltaksdeltakerHendelse = hendelser.first()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            tiltaksdeltakerHendelse.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerHendelsePostgresRepo
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo, testDataHelper.clock)
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknad = ObjectMother.nyInnvilgbarSøknad(
                personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId),
                sakId = sak.id,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = soknad,
            )
            val opprinneligTiltaksdeltakerHendelse = TiltaksdeltakerHendelse(
                id = TiltaksdeltakerHendelseId.random(),
                eksternDeltakerId = deltakerId,
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                internDeltakerId = soknad.tiltak.tiltaksdeltakerId,
                behandlingId = null,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerHendelse, "melding", TiltaksdeltakerHendelseKilde.TeamTiltak)

            tiltaksdeltakerService.behandleMottattTeamTiltakdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(teamTiltakDeltaker),
            )

            val hendelser = tiltaksdeltakerKafkaRepository.hentForEksternDeltakerId(deltakerId)
            hendelser shouldHaveSize 2
            val opprinnelig = tiltaksdeltakerKafkaRepository.hent(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.first { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            nyHendelse.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            nyHendelse.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            nyHendelse.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    private fun getArenaMeldingString() =
        """
           {
              "op_type": "U",
              "after": {
                "ANTALL_DAGER_PR_UKE": 2.0,
                "PROSENT_DELTID": 50.0,
                "DELTAKERSTATUSKODE": "GJENN",
                "DATO_FRA": "2024-10-14 00:00:00",
                "DATO_TIL": "2025-08-10 00:00:00",
                "EKSTERN_ID": null
              }
            } 
        """.trimIndent()

    private fun getArenaMeldingMedEksternIdString() =
        """
           {
              "op_type": "U",
              "after": {
                "ANTALL_DAGER_PR_UKE": 2.0,
                "PROSENT_DELTID": 50.0,
                "DELTAKERSTATUSKODE": "GJENN",
                "DATO_FRA": "2024-10-14 00:00:00",
                "DATO_TIL": "2025-08-10 00:00:00",
                "EKSTERN_ID": "9bedf708-1aa2-4be0-a561-cbe60ff2e9f9"
              }
            } 
        """.trimIndent()

    private fun getKometDeltaker(): DeltakerV1Dto =
        DeltakerV1Dto(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2024, 10, 14),
            sluttDato = LocalDate.of(2025, 8, 10),
            status = DeltakerV1Dto.DeltakerStatusDto(type = KometDeltakerStatusType.DELTAR),
            dagerPerUke = 2.0F,
            prosentStilling = 50.0F,
        )

    private fun getTeamTiltakDeltaker(): AvtaleDto =
        AvtaleDto(
            avtaleId = UUID.randomUUID(),
            hendelseType = AvtaleDto.HendelseType.ENDRET,
            avtaleStatus = AvtaleDto.AvtaleStatus.GJENNOMFØRES,
            startDato = LocalDate.of(2024, 10, 14),
            sluttDato = LocalDate.of(2025, 8, 10),
            stillingprosent = 80.0,
            antallDagerPerUke = 4.0,
            feilregistrert = false,
        )
}
