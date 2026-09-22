package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EksternOppgaveFakeRepoTest {
    @Test
    fun `første registrering beholdes også når et senere duplikat har annen sak og grunnlag`() {
        val repo = EksternOppgaveFakeRepo()
        val førsteSak = SakId.random()
        val annenSak = SakId.random()
        val første = referanse("123", førsteSak, LocalDateTime.of(2026, 3, 18, 12, 0))
        val duplikat = referanse("123", annenSak, LocalDateTime.of(2026, 3, 19, 12, 0))

        repo.lagre(første)
        repo.lagre(første)
        repo.lagre(duplikat)

        repo.hentForSakId(førsteSak) shouldBe listOf(første)
        repo.hentForSakId(annenSak) shouldBe emptyList()
    }

    @Test
    fun `filtrerer på sak og sorterer på tidspunkt og oppgave-id`() {
        val repo = EksternOppgaveFakeRepo()
        val sakId = SakId.random()
        val tidspunkt = LocalDateTime.of(2026, 3, 18, 12, 0)
        val eldste = referanse("999", sakId, tidspunkt.minusDays(1))
        val førsteVedLikTid = referanse("100", sakId, tidspunkt)
        val sisteVedLikTid = referanse("200", sakId, tidspunkt)
        val annenSak = referanse("001", SakId.random(), tidspunkt.minusDays(2))

        repo.hentForSakId(sakId) shouldBe emptyList()
        listOf(sisteVedLikTid, annenSak, førsteVedLikTid, eldste).forEach { repo.lagre(it) }

        repo.hentForSakId(sakId) shouldBe listOf(eldste, førsteVedLikTid, sisteVedLikTid)
        repo.hentForSakId(annenSak.sakId) shouldBe listOf(annenSak)
    }

    private fun referanse(oppgaveId: String, sakId: SakId, opprettet: LocalDateTime) = EksternOppgave(
        oppgaveId = OppgaveId(oppgaveId),
        sakId = sakId,
        opprettet = opprettet,
        grunnlag = Oppgavegrunnlag.EndretTiltaksdeltakelse(
            hendelseId = TiltaksdeltakerHendelseId.random(),
            tiltaksdeltakerId = TiltaksdeltakerId.random(),
            eksternDeltakerId = UUID.randomUUID().toString(),
            deltakelseFraOgMed = LocalDate.of(2026, 3, 17),
            deltakelseTilOgMed = null,
            dagerPerUke = 2.5f,
            deltakelsesprosent = 50.0f,
            deltakerstatus = TiltakDeltakerstatus.Deltar,
        ),
        tilleggstekst = "Endret tiltaksdeltakelse",
    )
}
