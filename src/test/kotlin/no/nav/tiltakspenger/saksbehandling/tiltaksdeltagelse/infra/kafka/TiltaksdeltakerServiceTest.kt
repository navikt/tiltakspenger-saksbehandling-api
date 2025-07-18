package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.komet.DeltakerV1Dto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class TiltaksdeltakerServiceTest {
    private val arenaDeltakerMapper = ArenaDeltakerMapper()

    @Test
    fun `behandleMottattArenadeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
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
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val fnr = Fnr.Companion.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    søknadstiltak = ObjectMother.søknadstiltak(id = id),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
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
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak og melding med oppgaveId - lagrer og beholder oppgaveId`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
            val deltakerId = "123456789"
            val id = "TA$deltakerId"
            val fnr = Fnr.Companion.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    søknadstiltak = ObjectMother.søknadstiltak(id = id),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
            )
            val oppgaveSistSjekket = LocalDateTime.now()
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
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
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
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val fnr = Fnr.Companion.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId.toString()),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
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
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak og melding med oppgaveId - lagrer og beholder oppgaveId`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
            val soknadRepo = testDataHelper.søknadRepo
            val tiltaksdeltakerService =
                TiltaksdeltakerService(tiltaksdeltakerKafkaRepository, soknadRepo, arenaDeltakerMapper)
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val fnr = Fnr.Companion.random()
            val sak = ObjectMother.nySak(fnr = fnr)
            testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
                søknad = ObjectMother.nySøknad(
                    personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                    søknadstiltak = ObjectMother.søknadstiltak(id = deltakerId.toString()),
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                ),
            )
            val oppgaveSistSjekket = LocalDateTime.now()
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
                "DATO_TIL": "2025-08-10 00:00:00"
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
}
