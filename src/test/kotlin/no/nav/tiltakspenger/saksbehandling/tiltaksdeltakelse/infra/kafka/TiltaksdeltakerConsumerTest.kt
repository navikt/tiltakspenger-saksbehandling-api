package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak.TeamTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.LagretTiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerEndringerForEksternId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Tilstanden bygges gjennom prodstiene: sak og søknad opprettes via routene, og meldingene kommer inn via consumerne for Arena, Komet og Team Tiltak slik de gjør i nais.
 */
class TiltaksdeltakerConsumerTest {

    @Test
    fun `arena - finnes ingen tiltaksdeltaker - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId("TA$deltakerId").shouldBeEmpty()
        }
    }

    @Test
    fun `arena - finnes tiltaksdeltaker, men ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            // Deltakeren må peke på en sak (NOT NULL), men ingen søknadstiltak kobler den — derfor ignoreres hendelsen likevel.
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, Fnr.random())
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            tac.tiltakContext.tiltaksdeltakerRepo.lagre(
                id = tiltaksdeltakelse.internDeltakelseId,
                eksternId = tiltaksdeltakelse.eksternDeltakelseId,
                tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
                sakId = sakId,
            )

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).shouldBeEmpty()
        }
    }

    @Test
    fun `arena - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).single()
            tiltaksdeltakerHendelse.skalVæreMottattMelding(getArenaMeldingString(), TiltaksdeltakerHendelseKilde.Arena, sak.id, tiltaksdeltakelse.internDeltakelseId)

            // Consumeren markerer deltakeren med ubehandlet endring, som OppdatertTiltaksdeltakelseJobb plukker opp.
            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(id).shouldNotBeNull()
            deltaker.sakId shouldBe sak.id
            deltaker.sisteUbehandletEndringTidspunkt shouldNotBe null
        }
    }

    @Test
    fun `arena - finnes sak, har eksternId - oppdaterer eksternId og lagrer`() {
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

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).shouldBeEmpty()
            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(nyEksternId.toString()).single()
            tiltaksdeltakerHendelse.skalVæreMottattMelding(getArenaMeldingMedEksternIdString(nyEksternId), TiltaksdeltakerHendelseKilde.Arena, sak.id, tiltaksdeltakelse.internDeltakelseId)
        }
    }

    @Test
    fun `arena - finnes sak for arena-eksternId - lagrer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val nyEksternId = UUID.randomUUID()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = nyEksternId.toString())
            opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingMedEksternIdString(nyEksternId))

            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(nyEksternId.toString()) shouldNotBe null
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(id) shouldBe null

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(nyEksternId.toString()).shouldBeEmpty()
            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).shouldBeEmpty()
        }
    }

    @Test
    fun `arena - lagrer ny hendelse uten å endre historikken`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())
            val opprinneligTiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).single()

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, getArenaMeldingString())

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id)
            hendelser.size shouldBe 2
            hendelser.single { it.id == opprinneligTiltaksdeltakerHendelse.id } shouldBe opprinneligTiltaksdeltakerHendelse
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.skalVæreMottattMelding(getArenaMeldingString(), TiltaksdeltakerHendelseKilde.Arena, sak.id, tiltaksdeltakelse.internDeltakelseId)
        }
    }

    @Test
    fun `komet - finnes ingen tiltaksdeltaker - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()

            tac.tiltaksdeltakerKometConsumer.consume(kometDeltaker.id, objectMapper.writeValueAsString(kometDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(kometDeltaker.id.toString()).shouldBeEmpty()
        }
    }

    @Test
    fun `komet - finnes tiltaksdeltaker, men ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString())
            // Deltakeren må peke på en sak (NOT NULL), men ingen søknadstiltak kobler den — derfor ignoreres hendelsen likevel.
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, Fnr.random())
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            tac.tiltakContext.tiltaksdeltakerRepo.lagre(
                id = tiltaksdeltakelse.internDeltakelseId,
                eksternId = tiltaksdeltakelse.eksternDeltakelseId,
                tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
                sakId = sakId,
            )

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId.toString()).shouldBeEmpty()
        }
    }

    @Test
    fun `komet - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString())
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId.toString()).single()
            tiltaksdeltakerHendelse.skalVæreMottattMelding(objectMapper.writeValueAsString(kometDeltaker), TiltaksdeltakerHendelseKilde.Komet, sak.id, tiltaksdeltakelse.internDeltakelseId)

            // Consumeren markerer deltakeren med ubehandlet endring, som OppdatertTiltaksdeltakelseJobb plukker opp.
            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakerId.toString()).shouldNotBeNull()
            deltaker.sakId shouldBe sak.id
            deltaker.sisteUbehandletEndringTidspunkt shouldNotBe null
        }
    }

    @Test
    fun `komet - lagrer ny hendelse uten å endre historikken`() {
        withTestApplicationContextAndPostgres { tac ->
            val kometDeltaker = getKometDeltaker()
            val deltakerId = kometDeltaker.id
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString())
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))
            val opprinneligTiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId.toString()).single()

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, objectMapper.writeValueAsString(kometDeltaker))

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId.toString())
            hendelser.size shouldBe 2
            hendelser.single { it.id == opprinneligTiltaksdeltakerHendelse.id } shouldBe opprinneligTiltaksdeltakerHendelse
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.skalVæreMottattMelding(objectMapper.writeValueAsString(kometDeltaker), TiltaksdeltakerHendelseKilde.Komet, sak.id, tiltaksdeltakelse.internDeltakelseId)
        }
    }

    @Test
    fun `team tiltak - finnes ingen tiltaksdeltaker - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId).shouldBeEmpty()
        }
    }

    @Test
    fun `team tiltak - finnes tiltaksdeltaker, men ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId)
            // Deltakeren må peke på en sak (NOT NULL), men ingen søknadstiltak kobler den — derfor ignoreres hendelsen likevel.
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, Fnr.random())
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            tac.tiltakContext.tiltaksdeltakerRepo.lagre(
                id = tiltaksdeltakelse.internDeltakelseId,
                eksternId = tiltaksdeltakelse.eksternDeltakelseId,
                tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
                sakId = sakId,
            )

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId).shouldBeEmpty()
        }
    }

    @Test
    fun `team tiltak - finnes sak, ikke lagret melding - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            val tiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId).single()
            tiltaksdeltakerHendelse.skalVæreMottattMelding(objectMapper.writeValueAsString(teamTiltakDeltaker), TiltaksdeltakerHendelseKilde.TeamTiltak, sak.id, tiltaksdeltakelse.internDeltakelseId)

            // Consumeren markerer deltakeren med ubehandlet endring, som OppdatertTiltaksdeltakelseJobb plukker opp.
            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakerId).shouldNotBeNull()
            deltaker.sakId shouldBe sak.id
            deltaker.sisteUbehandletEndringTidspunkt shouldNotBe null
        }
    }

    @Test
    fun `team tiltak - lagrer ny hendelse uten å endre historikken`() {
        withTestApplicationContextAndPostgres { tac ->
            val teamTiltakDeltaker = getTeamTiltakDeltaker()
            val deltakerId = teamTiltakDeltaker.avtaleId.toString()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId)
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = tiltaksdeltakelse)
            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))
            val opprinneligTiltaksdeltakerHendelse = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId).single()

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, objectMapper.writeValueAsString(teamTiltakDeltaker))

            val hendelser = tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId)
            hendelser.size shouldBe 2
            hendelser.single { it.id == opprinneligTiltaksdeltakerHendelse.id } shouldBe opprinneligTiltaksdeltakerHendelse
            val nyHendelse = hendelser.single { it.id != opprinneligTiltaksdeltakerHendelse.id }
            nyHendelse.skalVæreMottattMelding(objectMapper.writeValueAsString(teamTiltakDeltaker), TiltaksdeltakerHendelseKilde.TeamTiltak, sak.id, tiltaksdeltakelse.internDeltakelseId)
        }
    }

    @Test
    fun `arena - slettet deltakelse uten deltakerinfo - lagrer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = arenaDeltakerId()
            val id = "TA$deltakerId"
            opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = id))

            tac.tiltaksdeltakerArenaConsumer.consume(deltakerId, """{"op_type":"D","after":null}""")

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(id).shouldBeEmpty()
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(id).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe null
        }
    }

    @Test
    fun `komet - tombstone - lagrer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = UUID.randomUUID()
            opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId.toString()))

            tac.tiltaksdeltakerKometConsumer.consume(deltakerId, null)

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId.toString()).shouldBeEmpty()
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakerId.toString()).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe null
        }
    }

    @Test
    fun `team tiltak - tombstone - lagrer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakerId = UUID.randomUUID().toString()
            opprettSakOgSøknad(tac = tac, fnr = Fnr.random(), tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = deltakerId))

            tac.tiltaksdeltakerTeamTiltakConsumer.consume(deltakerId, null)

            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(deltakerId).shouldBeEmpty()
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakerId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe null
        }
    }

    /** Meldingen lagres ordrett, og jobben har ennå ikke behandlet den. */
    private fun LagretTiltaksdeltakerEndring.skalVæreMottattMelding(
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
        sakId: SakId,
        tiltaksdeltakerId: TiltaksdeltakerId,
    ) {
        this.verdi shouldBe melding
        this.kilde shouldBe kilde
        this.sakId shouldBe sakId.toString()
        this.tiltaksdeltakerId shouldBe tiltaksdeltakerId.toString()
        this.endring shouldBe null
        this.behandlingId shouldBe null
        this.oppgaveId shouldBe null
        this.behandletTidspunkt shouldBe null
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

    private fun getKometDeltaker(): KometTiltakHendelseDTO =
        KometTiltakHendelseDTO(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2024, 10, 14),
            sluttDato = LocalDate.of(2025, 8, 10),
            status = KometTiltakHendelseDTO.DeltakerStatusDto(type = KometDeltakerStatusTypeDTO.DELTAR),
            dagerPerUke = 2.0F,
            prosentStilling = 50.0F,
        )

    private fun getTeamTiltakDeltaker(): TeamTiltakHendelseDTO =
        TeamTiltakHendelseDTO(
            avtaleId = UUID.randomUUID(),
            hendelseType = TeamTiltakHendelseDTO.HendelseType.ENDRET,
            avtaleStatus = TeamTiltakHendelseDTO.AvtaleStatus.GJENNOMFØRES,
            startDato = LocalDate.of(2024, 10, 14),
            sluttDato = LocalDate.of(2025, 8, 10),
            stillingprosent = 80.0,
            antallDagerPerUke = 4.0,
            feilregistrert = false,
        )
}
