package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksdeltakerKafkaDbTest {
    private val lagretTiltaksdeltakelse = Tiltaksdeltagelse(
        eksternDeltagelseId = UUID.randomUUID().toString(),
        gjennomføringId = UUID.randomUUID().toString(),
        typeNavn = "Avklaring",
        typeKode = TiltakstypeSomGirRett.AVKLARING,
        rettPåTiltakspenger = true,
        deltakelsesperiode = Periode(fraOgMed = 5.januar(2025), tilOgMed = 5.april(2025)),
        deltakelseStatus = TiltakDeltakerstatus.Deltar,
        deltakelseProsent = 50.0F,
        antallDagerPerUke = 2.0F,
        kilde = Tiltakskilde.Komet,
    )

    @Test
    fun `tiltaksdeltakelseErEndret - ingen endring - returnerer false`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb()

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret startdato - returnerer true`() {
        val tiltaksdeltakerKafkaDb =
            getTiltaksdeltakerKafkaDb(fom = lagretTiltaksdeltakelse.deltakelsesperiode.fraOgMed.minusDays(3))

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret sluttdato - returnerer true`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(tom = lagretTiltaksdeltakelse.deltakelsesperiode.tilOgMed.plusMonths(1))

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret dager pr uke - returnerer true`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(dagerPerUke = 1F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret deltakelsesmengde - returnerer true`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(deltakelsesprosent = 60F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - lagret deltakelsesmengde er 0 og mottatt er null - returnerer false`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(deltakelsesprosent = null)
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(deltakelseProsent = 0.0F)

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret status og sluttdato - returnerer true`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelsesperiode = Periode(
                fraOgMed = LocalDate.now().minusMonths(3),
                tilOgMed = LocalDate.now().plusWeeks(1),
            ),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelse.deltakelsesperiode.tilOgMed.minusWeeks(2),
            deltakerstatus = TiltakDeltakerstatus.HarSluttet,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er passert - returnerer false`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelsesperiode = Periode(
                fraOgMed = LocalDate.now().minusMonths(3),
                tilOgMed = LocalDate.now().minusDays(1),
            ),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelse.deltakelsesperiode.tilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Fullført,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er ikke passert - returnerer true`() {
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelsesperiode = Periode(
                fraOgMed = LocalDate.now().minusMonths(3),
                tilOgMed = LocalDate.now().plusWeeks(1),
            ),
        )
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            fom = tiltaksdeltakelse.deltakelsesperiode.fraOgMed,
            tom = tiltaksdeltakelse.deltakelsesperiode.tilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Fullført,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til avbrutt - returnerer true`() {
        val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
        )

        tiltaksdeltakerKafkaDb.tiltaksdeltakelseErEndret(lagretTiltaksdeltakelse) shouldBe true
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
