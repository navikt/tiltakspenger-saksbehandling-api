package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksdeltakerHendelseTest {
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
            internDeltakelseId = TiltaksdeltakerId.random(),
        )

    @Test
    fun `tiltaksdeltakelseErEndret - ingen endring - returnerer ingen endringer`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse()

        tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldBeNull()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret startdato - returnerer ENDRET_STARTDATO`() {
        val clock = TikkendeKlokke()
        val nyStartdato = lagretTiltaksdeltakelse.deltakelseFraOgMed!!.minusDays(3)
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(fom = nyStartdato)

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.EndretStartdato>()
        (endringer.first() as TiltaksdeltakerEndring.EndretStartdato).nyStartdato shouldBe nyStartdato
    }

    @Test
    fun `tiltaksdeltakelseErEndret - forlengelse - returnerer FORLENGELSE`() {
        val clock = TikkendeKlokke()
        val nySluttdato = lagretTiltaksdeltakelse.deltakelseTilOgMed!!.plusMonths(1)
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(tom = nySluttdato)

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.Forlengelse>()
        (endringer.first() as TiltaksdeltakerEndring.Forlengelse).nySluttdato shouldBe nySluttdato
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret dager pr uke - returnerer ENDRET_DELTAKELSESMENGDE`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(dagerPerUke = 1F)

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.EndretDeltakelsesmengde>()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret deltakelsesmengde - returnerer ENDRET_DELTAKELSESMENGDE`() {
        val clock = TikkendeKlokke()
        val nyDeltakelsesprosent = 60F
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(deltakelsesprosent = nyDeltakelsesprosent)

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.EndretDeltakelsesmengde>()
        (endringer.first() as TiltaksdeltakerEndring.EndretDeltakelsesmengde).nyDeltakelsesprosent shouldBe nyDeltakelsesprosent
    }

    @Test
    fun `tiltaksdeltakelseErEndret - lagret deltakelsesmengde er 0 og mottatt er null - returnerer ingen endringer`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(deltakelsesprosent = null)
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(deltakelseProsent = 0.0F)

        tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldBeNull()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - endret status og sluttdato - returnerer AVBRUTT_DELTAKELSE`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock).minusMonths(3),
            deltakelseTilOgMed = LocalDate.now(clock).plusWeeks(1),
        )
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed!!.minusWeeks(2),
            deltakerstatus = TiltakDeltakerstatus.HarSluttet,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.AvbruttDeltakelse>()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er passert - returnerer ingen endringer`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock).minusMonths(3),
            deltakelseTilOgMed = LocalDate.now(clock).minusDays(1),
        )
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Fullført,
        )

        tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldBeNull()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til deltar, startdato er passert - returnerer ingen endringer`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock).minusDays(1),
            deltakelseTilOgMed = LocalDate.now(clock).plusMonths(3),
        )
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = TiltakDeltakerstatus.Deltar,
        )

        tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldBeNull()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til deltar, startdato er i dag, forlengelse - returnerer FORLENGELSE`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock),
            deltakelseTilOgMed = LocalDate.now(clock).plusMonths(3),
        )
        val nySluttdato = tiltaksdeltakelse.deltakelseTilOgMed?.plusWeeks(1)
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = nySluttdato,
            deltakerstatus = TiltakDeltakerstatus.Deltar,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.Forlengelse>()
        (endringer.first() as TiltaksdeltakerEndring.Forlengelse).nySluttdato shouldBe nySluttdato
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til fullfort, sluttdato er ikke passert - returnerer ENDRET_STATUS`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock).minusMonths(3),
            deltakelseTilOgMed = LocalDate.now(clock).plusWeeks(1),
        )
        val nyStatus = TiltakDeltakerstatus.Fullført
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = tiltaksdeltakelse.deltakelseFraOgMed,
            tom = tiltaksdeltakelse.deltakelseTilOgMed,
            deltakerstatus = nyStatus,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.EndretStatus>()
        (endringer.first() as TiltaksdeltakerEndring.EndretStatus).nyStatus shouldBe nyStatus
    }

    @Test
    fun `tiltaksdeltakelseErEndret - kun endret status til avbrutt - returnerer AVBRUTT_DELTAKELSE`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.AvbruttDeltakelse>()
    }

    @Test
    fun `tiltaksdeltakelseErEndret - forlengelse og endret deltakelsesmengde - returnerer begge`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            tom = lagretTiltaksdeltakelse.deltakelseTilOgMed!!.plusMonths(1),
            dagerPerUke = 1F,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(lagretTiltaksdeltakelse, clock).shouldNotBeNull()
        endringer.size shouldBe 2
        endringer.any { it is TiltaksdeltakerEndring.Forlengelse } shouldBe true
        endringer.any { it is TiltaksdeltakerEndring.EndretDeltakelsesmengde } shouldBe true
    }

    @Test
    fun `tiltaksdeltakelseErEndret - blir ikke aktuell - returnerer IKKE_AKTUELL_DELTAKELSE`() {
        val clock = TikkendeKlokke()
        val tiltaksdeltakelse = lagretTiltaksdeltakelse.copy(
            deltakelseFraOgMed = LocalDate.now(clock).plusWeeks(1),
            deltakelseTilOgMed = LocalDate.now(clock).plusMonths(4),
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )
        val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
            fom = null,
            tom = null,
            deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
        )

        val endringer = tiltaksdeltakerHendelse.finnEndringer(tiltaksdeltakelse, clock).shouldNotBeNull()
        endringer shouldHaveSize 1
        endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.IkkeAktuellDeltakelse>()
    }
}

fun getTiltaksdeltakerHendelse(
    id: String = UUID.randomUUID().toString(),
    hendelseId: TiltaksdeltakerHendelseId = TiltaksdeltakerHendelseId.random(),
    fom: LocalDate? = 5.januar(2025),
    tom: LocalDate? = 5.april(2025),
    dagerPerUke: Float? = 2.0F,
    deltakelsesprosent: Float? = 50.0F,
    deltakerstatus: TiltakDeltakerstatus = TiltakDeltakerstatus.Deltar,
    sakId: SakId = SakId.random(),
    oppgaveId: OppgaveId? = null,
    oppgaveSistSjekket: LocalDateTime? = null,
    tiltaksdeltakerId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    kilde: TiltaksdeltakerHendelseKilde = TiltaksdeltakerHendelseKilde.Komet,
) =
    TiltaksdeltakerHendelse(
        id = hendelseId,
        deltakerId = id,
        deltakelseFraOgMed = fom,
        deltakelseTilOgMed = tom,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        deltakerstatus = deltakerstatus,
        sakId = sakId,
        oppgaveId = oppgaveId,
        oppgaveSistSjekket = oppgaveSistSjekket,
        tiltaksdeltakerId = tiltaksdeltakerId,
        behandlingId = null,
        kilde = kilde,
    )
