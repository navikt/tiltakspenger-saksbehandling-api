package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.InternOppgaveFakeRepo
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.annenTestsaksbehandler
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.testbegrunnelse
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.testsaksbehandler
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
        val tildelt = service.ta(oppgave.id, oppgave.versjon, testsaksbehandler).getOrFail()
        val nyttGrunnlag = grunnlag("Ny sluttdato")
        val oppdatert = service.opprettEllerOppdater(sakId, nyttGrunnlag).getOrFail()
        oppdatert.id shouldBe oppgave.id
        oppdatert.opprettet shouldBe oppgave.opprettet
        oppdatert.grunnlag shouldBe nyttGrunnlag
        oppdatert.saksbehandler shouldBe "Z123456"
        oppdatert.versjon shouldBe tildelt.versjon + 1
        repo.hentForSak(sakId) shouldBe listOf(oppdatert)

        service.løs(tildelt.id, tildelt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        service.leggTilbake(tildelt.id, tildelt.versjon, testsaksbehandler) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        repo.hent(oppgave.id) shouldBe oppdatert
    }

    @Test
    fun `eier kan gi fra seg oppgaven og ny eier kan forkaste den`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val tildelt = service.ta(ny.id, ny.versjon, testsaksbehandler).getOrFail()
        service.ta(ny.id, tildelt.versjon, annenTestsaksbehandler) shouldBe InternOppgaveFeil.AlleredeTildelt.left()
        service.leggTilbake(ny.id, tildelt.versjon, annenTestsaksbehandler) shouldBe InternOppgaveFeil.IkkeEier.left()
        service.løs(ny.id, tildelt.versjon, annenTestsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe InternOppgaveFeil.IkkeEier.left()
        val frigitt = service.leggTilbake(ny.id, tildelt.versjon, testsaksbehandler).getOrFail()
        frigitt.saksbehandler shouldBe null
        val overtatt = service.ta(ny.id, frigitt.versjon, annenTestsaksbehandler).getOrFail()
        val løst = service.løs(ny.id, overtatt.versjon, annenTestsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse).getOrFail()
        løst.erLøst shouldBe true
        repo.hent(ny.id) shouldBe løst
        service.ta(ny.id, løst.versjon, testsaksbehandler) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        service.leggTilbake(ny.id, løst.versjon, annenTestsaksbehandler) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        service.løs(ny.id, løst.versjon, annenTestsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        val neste = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        (neste.id != ny.id) shouldBe true
        neste.versjon shouldBe 0L
        repo.hent(ny.id) shouldBe løst
    }

    @Test
    fun `saksbehandler kan overta oppgaven fra en annen med gjeldende versjon`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        service.overta(ny.id, ny.versjon, annenTestsaksbehandler) shouldBe InternOppgaveFeil.IkkeTildelt.left()
        val tildelt = service.ta(ny.id, ny.versjon, testsaksbehandler).getOrFail()
        service.overta(ny.id, ny.versjon, annenTestsaksbehandler) shouldBe InternOppgaveFeil.OppgavenErEndret.left()
        service.overta(ny.id, tildelt.versjon, testsaksbehandler) shouldBe InternOppgaveFeil.KanIkkeOvertaFraSegSelv.left()
        val overtatt = service.overta(ny.id, tildelt.versjon, annenTestsaksbehandler).getOrFail()
        overtatt.saksbehandler shouldBe annenTestsaksbehandler.navIdent
        overtatt.versjon shouldBe tildelt.versjon + 1
        repo.hent(ny.id) shouldBe overtatt
        service.løs(ny.id, tildelt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe
            InternOppgaveFeil.OppgavenErEndret.left()
        service.løs(ny.id, overtatt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe
            InternOppgaveFeil.IkkeEier.left()
        val løst = service.løs(ny.id, overtatt.versjon, annenTestsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse).getOrFail()
        service.overta(ny.id, løst.versjon, testsaksbehandler) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
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
        service.ta(id, 0, testsaksbehandler) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.leggTilbake(id, 0, testsaksbehandler) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.overta(id, 0, testsaksbehandler) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.løs(id, 0, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        service.leggTilDialoginnlegg(id, testsaksbehandler, NonBlankString.create("Hei")) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
    }

    @Test
    fun `dialogen lagres uten versjonsbump og overlever senere endringer`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val tildelt = service.ta(ny.id, ny.versjon, testsaksbehandler).getOrFail()
        val medInnlegg = service.leggTilDialoginnlegg(ny.id, annenTestsaksbehandler, NonBlankString.create("Sjekk sluttdato")).getOrFail()
        medInnlegg.versjon shouldBe tildelt.versjon
        medInnlegg.dialog shouldBe listOf(Dialoginnlegg("Z654321", nå(clock), NonBlankString.create("Sjekk sluttdato")))
        repo.hent(ny.id) shouldBe medInnlegg

        val løst = service.løs(ny.id, tildelt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse).getOrFail()
        løst.dialog shouldBe medInnlegg.dialog
        repo.hent(ny.id) shouldBe løst
        service.leggTilDialoginnlegg(ny.id, annenTestsaksbehandler, NonBlankString.create("For sent")) shouldBe
            InternOppgaveFeil.OppgavenErLøst.left()
        repo.hent(ny.id) shouldBe løst
    }

    @Test
    fun `dialoginnlegg avvises når oppgaven løses mellom lesing og lagring`() {
        val ny = service.opprettEllerOppdater(sakId, grunnlag()).getOrFail()
        val tildelt = service.ta(ny.id, ny.versjon, testsaksbehandler).getOrFail()
        val konkurrerendeRepo = object : InternOppgaveRepo by repo {
            override fun leggTilDialoginnlegg(id: InternOppgaveId, innlegg: Dialoginnlegg, sessionContext: SessionContext?): Boolean {
                service.løs(id, tildelt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse).getOrFail()
                return repo.leggTilDialoginnlegg(id, innlegg, sessionContext)
            }
        }
        InternOppgaveService(konkurrerendeRepo, clock).leggTilDialoginnlegg(ny.id, annenTestsaksbehandler, NonBlankString.create("Hei")) shouldBe
            InternOppgaveFeil.OppgavenErLøst.left()
        repo.hent(ny.id)!!.dialog shouldBe emptyList()
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
                service.ta(ny.id, ny.versjon, annenTestsaksbehandler).getOrFail()
                return repo.oppdater(oppgave, forventetVersjon, sessionContext)
            }
        }
        InternOppgaveService(konkurrerendeRepo, clock).ta(ny.id, ny.versjon, testsaksbehandler) shouldBe
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
