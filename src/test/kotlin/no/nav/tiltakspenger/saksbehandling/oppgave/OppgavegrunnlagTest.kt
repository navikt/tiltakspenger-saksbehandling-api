package no.nav.tiltakspenger.saksbehandling.oppgave

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppgavegrunnlagTest {
    @Test
    fun `grunnlag maskerer personopplysninger og fritekst i toString`() {
        val grunnlag = Oppgavegrunnlag.EndretTiltaksdeltakelse(
            hendelseId = TiltaksdeltakerHendelseId.random(),
            tiltaksdeltakerId = TiltaksdeltakerId.random(),
            eksternDeltakerId = "ekstern-id",
            deltakelseFraOgMed = LocalDate.of(2026, 3, 17),
            deltakelseTilOgMed = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            deltakerstatus = TiltakDeltakerstatus.Deltar,
            tilleggstekst = "sensitiv fritekst",
        )
        grunnlag.toString() shouldBe "EndretTiltaksdeltakelse(*****)"

        val oppgave = EksternOppgave(
            oppgaveId = OppgaveId("123"),
            sakId = SakId.random(),
            opprettet = LocalDateTime.of(2026, 3, 18, 12, 0),
            grunnlag = grunnlag,
        )
        oppgave.toString().shouldNotContain("sensitiv fritekst")
        oppgave.toString().shouldNotContain("ekstern-id")
        oppgave.toString().shouldNotContain("2026-03-17")
        oppgave.toString().shouldNotContain(grunnlag.hendelseId.toString())
        oppgave.toString().shouldNotContain(grunnlag.tiltaksdeltakerId.toString())
    }
}
