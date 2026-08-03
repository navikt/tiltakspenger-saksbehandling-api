package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.DeltakerV1Dto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak.AvtaleDto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.getTiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelserForEksternId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Tilstanden bygges gjennom prodstiene: sak og søknad opprettes via routene, og meldingene kommer inn via consumerne for Arena, Komet og Team Tiltak slik de gjør i nais.
 */
class TiltaksdeltakerServiceTest {

    @Test
    fun `behandleMottattArenadeltaker - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId("TA$deltakerId").shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(id).single()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerHendelse.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak, har eksternId - oppdaterer eksternId og lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val nyEksternId = UUID.randomUUID()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingMedEksternIdString(nyEksternId))

            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldBe null
            val oppdatertTiltaksdeltaker = tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString())
            oppdatertTiltaksdeltaker?.id shouldBe tiltaksdeltakelse.internDeltakelseId
            oppdatertTiltaksdeltaker?.eksternId shouldBe nyEksternId.toString()
            oppdatertTiltaksdeltaker?.tiltakstype shouldBe tiltaksdeltakelse.typeKode.tilTiltakstype()
            oppdatertTiltaksdeltaker?.utdatertEksternId shouldBe id

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(id).shouldBeEmpty()
            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(nyEksternId.toString()).single()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            tiltaksdeltakerHendelse.dagerPerUke shouldBe 2.0F
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe 50.0F
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak for arena-eksternId - lagrer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val nyEksternId = UUID.randomUUID()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = nyEksternId.toString())
            opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingMedEksternIdString(nyEksternId))

            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString()) shouldNotBe null
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldBe null

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(nyEksternId.toString()).shouldBeEmpty()
            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(id).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattArenadeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            val opprinneligTiltaksdeltakerHendelse = lagreHendelseMedOppgave(
                tac = tac,
                eksternDeltakerId = id,
                tiltaksdeltakelse = tiltaksdeltakelse,
                sakId = sak.id,
                kilde = TiltaksdeltakerHendelseKilde.Arena,
            )

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(id)
            hendelser.size shouldBe 2
            val opprinnelig = tac.sessionFactory.hentTiltaksdeltakerHendelse(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe LocalDate.of(2024, 10, 14)
            nyHendelse.deltakelseTilOgMed shouldBe LocalDate.of(2025, 8, 10)
            nyHendelse.dagerPerUke shouldBe 2.0F
            nyHendelse.deltakelsesprosent shouldBe 50.0F
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()

            tac.tiltaksdeltakerKometConsumer.consume(kometDeltaker.id, objectMapper.writeValueAsString(kometDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(kometDeltaker.id.toString()).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString())
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(deltakerId.toString()).single()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            tiltaksdeltakerHendelse.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattKometdeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString())
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            val opprinneligTiltaksdeltakerHendelse = lagreHendelseMedOppgave(
                tac = tac,
                eksternDeltakerId = deltakerId.toString(),
                tiltaksdeltakelse = tiltaksdeltakelse,
                sakId = sak.id,
                kilde = TiltaksdeltakerHendelseKilde.Komet,
            )

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(deltakerId.toString())
            hendelser.size shouldBe 2
            val opprinnelig = tac.sessionFactory.hentTiltaksdeltakerHendelse(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe kometDeltaker.startDato
            nyHendelse.deltakelseTilOgMed shouldBe kometDeltaker.sluttDato
            nyHendelse.dagerPerUke shouldBe kometDeltaker.dagerPerUke
            nyHendelse.deltakelsesprosent shouldBe kometDeltaker.prosentStilling
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(deltakerId).shouldBeEmpty()
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(deltakerId).single()
            tiltaksdeltakerHendelse.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            tiltaksdeltakerHendelse.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            tiltaksdeltakerHendelse.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            tiltaksdeltakerHendelse.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            tiltaksdeltakerHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            tiltaksdeltakerHendelse.sakId shouldBe sak.id
            tiltaksdeltakerHendelse.oppgaveId shouldBe null
            tiltaksdeltakerHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    @Test
    fun `behandleMottattTeamTiltakdeltaker - finnes sak og melding med oppgaveId - lagrer ny hendelse uavhengig av eksisterende`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            val opprinneligTiltaksdeltakerHendelse = lagreHendelseMedOppgave(
                tac = tac,
                eksternDeltakerId = deltakerId,
                tiltaksdeltakelse = tiltaksdeltakelse,
                sakId = sak.id,
                kilde = TiltaksdeltakerHendelseKilde.TeamTiltak,
            )

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(deltakerId)
            hendelser.size shouldBe 2
            val opprinnelig = tac.sessionFactory.hentTiltaksdeltakerHendelse(opprinneligTiltaksdeltakerHendelse.id)
            opprinnelig shouldNotBe null
            opprinnelig?.oppgaveId shouldBe opprinneligTiltaksdeltakerHendelse.oppgaveId
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.deltakelseFraOgMed shouldBe teamTiltakDeltaker.startDato
            nyHendelse.deltakelseTilOgMed shouldBe teamTiltakDeltaker.sluttDato
            nyHendelse.dagerPerUke shouldBe teamTiltakDeltaker.antallDagerPerUke?.toFloat()
            nyHendelse.deltakelsesprosent shouldBe teamTiltakDeltaker.stillingprosent?.toFloat()
            nyHendelse.deltakerstatus shouldBe TiltakDeltakerstatus.Deltar
            nyHendelse.sakId shouldBe sak.id
            nyHendelse.oppgaveId shouldBe null
            nyHendelse.internDeltakerId shouldBe tiltaksdeltakelse.internDeltakelseId
        }
    }

    /**
     * Etablerer en tidligere behandlet hendelse med oppgave for deltakeren.
     * Oppgave-id-en settes normalt av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.EndretTiltaksdeltakerJobb]; her lagres den direkte for å slippe å dra i gang hele jobben, jf. samme mønster i EndretTiltaksdeltakerJobbTest.
     */
    private fun lagreHendelseMedOppgave(
        tac: TestApplicationContextMedPostgres,
        eksternDeltakerId: String,
        tiltaksdeltakelse: Tiltaksdeltakelse,
        sakId: SakId,
        kilde: TiltaksdeltakerHendelseKilde,
    ) = getTiltaksdeltakerHendelse(
        id = eksternDeltakerId,
        fom = LocalDate.of(2024, 10, 14),
        tom = LocalDate.of(2025, 1, 10),
        dagerPerUke = 3.0F,
        deltakelsesprosent = 60.0F,
        deltakerstatus = TiltakDeltakerstatus.HarSluttet,
        sakId = sakId,
        oppgaveId = ObjectMother.oppgaveId(),
        tiltaksdeltakerId = tiltaksdeltakelse.internDeltakelseId,
    ).also { tac.tiltaksdeltakerHendelsePostgresRepo.lagre(it, "melding", kilde) }

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

    private fun getArenaMeldingMedEksternIdString(eksternId: UUID) =
        """
           {
              "op_type": "U",
              "after": {
                "ANTALL_DAGER_PR_UKE": 2.0,
                "PROSENT_DELTID": 50.0,
                "DELTAKERSTATUSKODE": "GJENN",
                "DATO_FRA": "2024-10-14 00:00:00",
                "DATO_TIL": "2025-08-10 00:00:00",
                "EKSTERN_ID": "$eksternId"
              }
            }
        """.trimIndent()

    private fun arenaDeltakerId() = (100_000_000..999_999_999).random().toString()

    private fun getKometDeltaker(): DeltakerV1Dto =
        DeltakerV1Dto(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2024, 10, 14),
            sluttDato = LocalDate.of(2025, 8, 10),
            status = DeltakerV1Dto.DeltakerStatusDto(type = KometDeltakerStatusTypeDTO.DELTAR),
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
