package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.InternOppgaveFakeRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InternOppgaveServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var repo: InternOppgaveFakeRepo
    private lateinit var service: InternOppgaveService
    private lateinit var sakId: SakId
    private lateinit var deltakerId: TiltaksdeltakerId

    @BeforeEach
    fun setUp() {
        repo = InternOppgaveFakeRepo()
        service = InternOppgaveService(repo, clock)
        sakId = SakId.random()
        deltakerId = TiltaksdeltakerId.random()
    }

    @Test
    fun `systemet oppdaterer samme åpne oppgave og bevarer eier`() {
        val oppgave = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val tildelt = service.ta(oppgave.id, oppgave.versjon, "Z123456").getOrFail()
        val nyttGrunnlag = grunnlag("Ny sluttdato")
        val oppdatert = service.opprettEllerOppdater(sakId, nyttGrunnlag).getOrFail()
        oppdatert.id shouldBe oppgave.id
        oppdatert.opprettet shouldBe oppgave.opprettet
        oppdatert.grunnlag shouldBe nyttGrunnlag
        oppdatert.saksbehandler shouldBe "Z123456"
        oppdatert.versjon shouldBe tildelt.versjon + 1
        repo.hentForSak(sakId) shouldBe listOf(oppdatert)

        service.løs(tildelt.id, tildelt.versjon, "Z123456", InternOppgaveløsning.Forkastet) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        service.leggTilbake(tildelt.id, tildelt.versjon, "Z123456") shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        repo.hent(oppgave.id) shouldBe oppdatert
    }

    @Test
    fun `eier kan gi fra seg oppgaven og ny eier kan forkaste den`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val tildelt = service.ta(ny.id, ny.versjon, "Z123456").getOrFail()
        service.ta(ny.id, tildelt.versjon, "Z654321") shouldBe InternOppgaveFeil.AlleredeTildelt.left()
        service.leggTilbake(ny.id, tildelt.versjon, "Z654321") shouldBe InternOppgaveFeil.IkkeEier.left()
        service.løs(ny.id, tildelt.versjon, "Z654321", InternOppgaveløsning.Forkastet) shouldBe InternOppgaveFeil.IkkeEier.left()
        val frigitt = service.leggTilbake(ny.id, tildelt.versjon, "Z123456").getOrFail()
        frigitt.saksbehandler shouldBe null
        val overtatt = service.ta(ny.id, frigitt.versjon, "Z654321").getOrFail()
        val løst = service.løs(ny.id, overtatt.versjon, "Z654321", InternOppgaveløsning.Forkastet).getOrFail()
        løst.erLøst shouldBe true
        repo.hent(ny.id) shouldBe løst
        service.ta(ny.id, løst.versjon, "Z123456") shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        service.leggTilbake(ny.id, løst.versjon, "Z654321") shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        service.løs(ny.id, løst.versjon, "Z654321", InternOppgaveløsning.Forkastet) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        val neste = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        (neste.id != ny.id) shouldBe true
        neste.versjon shouldBe 0L
        repo.hent(ny.id) shouldBe løst
    }

    @Test
    fun `oppgaver avgrenses både på sak og deltaker`() {
        val første = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val annenSak = service.opprettEllerOppdater(SakId.random(), grunnlag()).getOrFail()
        val annenDeltaker = service.opprettEllerOppdater(
            sakId,
            InternOppgaveGrunnlag.EndretTiltaksdeltakelse(TiltaksdeltakerId.random(), TiltaksdeltakerHendelseId.random(), "Endring"),
        ).getOrFail()
        setOf(første.id, annenSak.id, annenDeltaker.id).size shouldBe 3
        repo.hentForSak(sakId).toSet() shouldBe setOf(første, annenDeltaker)
    }

    @Test
    fun `ukjent oppgave gir eksplisitt feil`() {
        val id = InternOppgaveId.random()
        service.ta(id, 0, "Z123456") shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.leggTilbake(id, 0, "Z123456") shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.løs(id, 0, "Z123456", InternOppgaveløsning.Forkastet) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
    }

    @Test
    fun `samtidig opprettelse gir konflikt og ikke falsk suksess`() {
        val konkurrerendeRepo = object : InternOppgaveRepo by repo {
            override fun opprett(oppgave: InternOppgave, sessionContext: SessionContext?): Boolean {
                service.opprettEllerOppdater(sakId, grunnlag("Konkurrerende endring")).getOrFail()
                return repo.opprett(oppgave, sessionContext)
            }
        }
        InternOppgaveService(konkurrerendeRepo, clock).opprettEllerOppdater(sakId, grunnlag()) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        repo.hentForSak(sakId).single().grunnlag.let { it as InternOppgaveGrunnlag.EndretTiltaksdeltakelse }
            .beskrivelse shouldBe "Konkurrerende endring"
    }

    @Test
    fun `endring mellom lesing og lagring overskriver ikke konkurrenten`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val konkurrerendeRepo = object : InternOppgaveRepo by repo {
            override fun oppdater(oppgave: InternOppgave, forventetVersjon: Long, sessionContext: SessionContext?): Boolean {
                service.ta(ny.id, ny.versjon, "Z654321").getOrFail()
                return repo.oppdater(oppgave, forventetVersjon, sessionContext)
            }
        }
        InternOppgaveService(konkurrerendeRepo, clock).ta(ny.id, ny.versjon, "Z123456") shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        repo.hent(ny.id)!!.saksbehandler shouldBe "Z654321"
    }

    @Test
    fun `samtidig systemoppdatering overskriver ikke nytt grunnlag`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val sisteGrunnlag = grunnlag("Nyeste kjente endring")
        val konkurrerendeRepo = object : InternOppgaveRepo by repo {
            override fun oppdater(oppgave: InternOppgave, forventetVersjon: Long, sessionContext: SessionContext?): Boolean {
                service.opprettEllerOppdater(sakId, sisteGrunnlag).getOrFail()
                return repo.oppdater(oppgave, forventetVersjon, sessionContext)
            }
        }
        InternOppgaveService(konkurrerendeRepo, clock).opprettEllerOppdater(sakId, grunnlag("Eldre endring")) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        repo.hent(ny.id)!!.grunnlag shouldBe sisteGrunnlag
    }

    private fun grunnlag(beskrivelse: String = "Endret deltakelse") = InternOppgaveGrunnlag.EndretTiltaksdeltakelse(
        tiltaksdeltakerId = deltakerId,
        hendelseId = TiltaksdeltakerHendelseId.random(),
        beskrivelse = beskrivelse,
    )
}
