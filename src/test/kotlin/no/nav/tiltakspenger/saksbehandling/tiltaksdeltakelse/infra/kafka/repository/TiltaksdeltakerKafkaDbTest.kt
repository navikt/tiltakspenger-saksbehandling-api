package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.ULID_PREFIX_TILTAKSDELTAKER
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksdeltakerKafkaDbTest {
    private val lagretTiltaksdeltakelse =
        Tiltaksdeltakelse(
            eksternDeltakelseId = UUID.randomUUID().toString(),
            gjennomføringId = UUID.randomUUID().toString(),
            typeNavn = "Avklaring",
            typeKode = TiltakstypeSomGirRett.AVKLARING,
            rettPåTiltakspenger = true,
            deltakelseFraOgMed = 5.januar(2025),
            deltakelseTilOgMed = 5.april(2025),
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
            deltakelseProsent = 50.0F,
            antallDagerPerUke = 2.0F,
            kilde = Tiltakskilde.Komet,
            deltidsprosentGjennomforing = 100.0,
            internDeltakelseId = random(ULID_PREFIX_TILTAKSDELTAKER).toString(),
        )

    @Test
    fun `tiltaksdeltakelseErEndret - ingen endring - returnerer ingen endringer`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb()

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe emptyList()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret startdato - returnerer ENDRET_STARTDATO`() {
        val tiltaksdeltakerKafkaDb =
            getTiltaksdeltakerKafkaDb(fom = lagretTiltaksdeltakelse.deltakelseFraOgMed!!.minusDays(3))

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.ENDRET_STARTDATO)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - forlengelse - returnerer FORLENGELSE`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(tom = lagretTiltaksdeltakelse.deltakelseTilOgMed!!.plusMonths(1))

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.FORLENGELSE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret dager pr uke - returnerer ENDRET_DELTAKELSESMENGDE`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(dagerPerUke = 1F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret deltakelsesmengde - returnerer ENDRET_DELTAKELSESMENGDE`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(deltakelsesprosent = 60F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - lagret deltakelsesmengde er 0 og mottatt er null - returnerer ingen endringer`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(deltakelsesprosent = null)
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(deltakelseProsent = 0.0F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe emptyList()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret status og sluttdato - returnerer AVBRUTT_DELTAKELSE`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now().minusMonths(3),
            deltakelseTilOgMed = LocalDate.now().plusWeeks(1),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed!!.minusWeeks(2),
            deltakerstatus = TiltakDeltakerstatus.HarSluttet,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er passert - returnerer ingen endringer`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now().minusMonths(3),
            deltakelseTilOgMed = LocalDate.now().minusDays(1),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Fullført,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe emptyList()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til deltar, startdato er passert - returnerer ingen endringer`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now().minusDays(1),
            deltakelseTilOgMed = LocalDate.now().plusMonths(3),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Deltar,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe emptyList()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til deltar, startdato er i dag, forlengelse - returnerer FORLENGELSE`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(),
            deltakelseTilOgMed = LocalDate.now().plusMonths(3),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed?.plusWeeks(1),
            deltakerstatus = TiltakDeltakerstatus.Deltar,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.FORLENGELSE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er ikke passert - returnerer ENDRET_STATUS`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now().minusMonths(3),
            deltakelseTilOgMed = LocalDate.now().plusWeeks(1),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Fullført,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.ENDRET_STATUS)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til avbrutt - returnerer AVBRUTT_DELTAKELSE`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE)
    }

    @Test
    fun `tiltaksdeltakelseErEndret - forlengelse og endret deltakelsesmengde - returnerer begge`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            tom = lagretTiltaksdeltakelse.deltakelseTilOgMed!!.plusMonths(1),
            dagerPerUke = 1F,
        )

        val endringer = tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse).sorted()
        endringer.size shouldBe 2
        endringer.first() shouldBe TiltaksdeltakerEndring.FORLENGELSE
        endringer[1] shouldBe TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE
    }

    @Test
    fun `tiltaksdeltakelseErEndret - blir ikke aktuell - returnerer IKKE_AKTUELL_DELTAKELSE`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now().plusWeeks(1),
            deltakelseTilOgMed = LocalDate.now().plusMonths(4),
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = null,
            tom = null,
            deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe listOf(TiltaksdeltakerEndring.IKKE_AKTUELL_DELTAKELSE)
    }
}

fun getTiltaksdeltakerKafkaDb(
    id: String = UUID.randomUUID().toString(),
    fom: LocalDate? = 5.januar(2025),
    tom: LocalDate? = 5.april(2025),
    dagerPerUke: Float? = 2.0F,
    deltakelsesprosent: Float? = 50.0F,
    deltakerstatus: TiltakDeltakerstatus = TiltakDeltakerstatus.Deltar,
    sakId: SakId = SakId.random(),
    oppgaveId: OppgaveId? = null,
    oppgaveSistSjekket: LocalDateTime? = null,
) =
    TiltaksdeltakerKafkaDb(
        id = id,
        deltakelseFraOgMed = fom,
        deltakelseTilOgMed = tom,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        deltakerstatus = deltakerstatus,
        sakId = sakId,
        oppgaveId = oppgaveId,
        oppgaveSistSjekket = oppgaveSistSjekket,
    )
