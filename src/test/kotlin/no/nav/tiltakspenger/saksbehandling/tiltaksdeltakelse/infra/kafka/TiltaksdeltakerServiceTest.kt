package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.DeltakerV1Dto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak.AvtaleDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class TiltaksdeltakerServiceTest {
    private val arenaDeltakerMapper = ArenaDeltakerMapper()

    @Test
    fun `behandleMottattArenadeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
            val deltakerId = "123456789"

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingString())

            tiltaksdeltakerKafkaRepository.hent(deltakerId) shouldBe null
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, har eksternId - oppdaterer eksternId og lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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

            tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(nyEksternId.toString())
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, har eksternId, arbeidstrening - oppdaterer ikke eksternId, lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val nyEksternId = UUID.fromString("9bedf708-1aa2-4be0-a561-cbe60ff2e9f9")
            val fnr = Fnr.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            val soknadstiltak = ObjectMother.søknadstiltak(id = id, typeKode = TiltakResponsDTO.TiltakType.ARBTREN)
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

            tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString()) shouldBe null
            tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldNotBe null

            tiltaksdeltakerKafkaRepository.hent(nyEksternId.toString()) shouldBe null
            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak for arena-eksternId - lagrer ikke`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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

            tiltaksdeltakerKafkaRepository.hent(nyEksternId.toString()) shouldBe null
            tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak og melding med oppgaveId - lagrer og beholder oppgaveId`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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
            val oppgaveSistSjekket = nå(testDataHelper.clock)
            val opprinneligTiltaksdeltakerKafkaDb = TiltaksdeltakerKafkaDb(
                id = id,
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                oppgaveSistSjekket = oppgaveSistSjekket,
                tiltaksdeltakerId = soknad.tiltak.tiltaksdeltakerId,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerKafkaDb, "melding")

            tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId, getArenaMeldingString())

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe opprinneligTiltaksdeltakerKafkaDb.oppgaveId
            tiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe oppgaveSistSjekket.truncatedTo(
                ChronoUnit.MINUTES,
            )
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id

            tiltaksdeltakerService.behandleMottattKometdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(kometDeltaker),
            )

            tiltaksdeltakerKafkaRepository.hent(deltakerId.toString()) shouldBe null
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(deltakerId.toString())
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak og melding med oppgaveId - lagrer og beholder oppgaveId`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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
            val oppgaveSistSjekket = nå(testDataHelper.clock)
            val opprinneligTiltaksdeltakerKafkaDb = TiltaksdeltakerKafkaDb(
                id = deltakerId.toString(),
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                oppgaveSistSjekket = oppgaveSistSjekket,
                tiltaksdeltakerId = soknad.tiltak.tiltaksdeltakerId,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerKafkaDb, "melding")

            tiltaksdeltakerService.behandleMottattKometdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(kometDeltaker),
            )

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(deltakerId.toString())
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe opprinneligTiltaksdeltakerKafkaDb.oppgaveId
            tiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe oppgaveSistSjekket.truncatedTo(
                ChronoUnit.MINUTES,
            )
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()

            tiltaksdeltakerService.behandleMottattTeamTiltakdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(teamTiltakDeltaker),
            )

            tiltaksdeltakerKafkaRepository.hent(deltakerId) shouldBe null
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(deltakerId)
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak og melding med oppgaveId - lagrer og beholder oppgaveId`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerRepo = testDataHelper.tiltaksdeltakerRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper, tiltaksdeltakerRepo)
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
            val oppgaveSistSjekket = nå(testDataHelper.clock)
            val opprinneligTiltaksdeltakerKafkaDb = TiltaksdeltakerKafkaDb(
                id = deltakerId,
                deltakelseFraOgMed = LocalDate.of(2024, 10, 14),
                deltakelseTilOgMed = LocalDate.of(2025, 1, 10),
                dagerPerUke = 3.0F,
                deltakelsesprosent = 60.0F,
                deltakerstatus = TiltakDeltakerstatus.HarSluttet,
                sakId = sak.id,
                oppgaveId = ObjectMother.oppgaveId(),
                oppgaveSistSjekket = oppgaveSistSjekket,
                tiltaksdeltakerId = soknad.tiltak.tiltaksdeltakerId,
            )
            tiltaksdeltakerKafkaRepository.lagre(opprinneligTiltaksdeltakerKafkaDb, "melding")

            tiltaksdeltakerService.behandleMottattTeamTiltakdeltaker(
                deltakerId,
                objectMapper.writeValueAsString(teamTiltakDeltaker),
            )

            val tiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(deltakerId)
            tiltaksdeltakerKafkaDb shouldNotBe null
            tiltaksdeltakerKafkaDb?.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            tiltaksdeltakerKafkaDb?.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            tiltaksdeltakerKafkaDb?.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            tiltaksdeltakerKafkaDb?.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            tiltaksdeltakerKafkaDb?.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerKafkaDb?.sakId shouldBe sak.id
            tiltaksdeltakerKafkaDb?.oppgaveId shouldBe opprinneligTiltaksdeltakerKafkaDb.oppgaveId
            tiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe oppgaveSistSjekket.truncatedTo(
                ChronoUnit.MINUTES,
            )
            tiltaksdeltakerKafkaDb?.tiltaksdeltakerId shouldBe soknad.tiltak.tiltaksdeltakerId
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
