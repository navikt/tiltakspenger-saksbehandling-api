package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import arrow.core.left
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveFeil
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveService
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Saker og revurderinger opprettes gjennom route-byggerne.
 * InternOppgaveService er modulens bevisste inngang inntil egne routes og applikasjonskobling finnes.
 * Testene injiserer derfor repoet direkte i servicen uten å innføre slik kobling.
 */
class InternOppgavePostgresRepoTest {

    @Test
    fun `oppretter tar legger tilbake og forkaster en oppgave med bevart historikk`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()

            repo.hentForSak(sak.id).shouldBeEmpty()
            repo.hent(InternOppgaveId.random()) shouldBe null
            repo.hentForSak(SakId.random()).shouldBeEmpty()
            repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel) shouldBe null

            val opprettet = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            opprettet.versjon shouldBe 0L
            repo.hent(opprettet.id) shouldBe opprettet
            repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel) shouldBe opprettet

            val tildelt = service.ta(opprettet.id, opprettet.versjon, "Z123456").getOrFail()
            tildelt.versjon shouldBe 1L
            repo.hent(tildelt.id) shouldBe tildelt
            tildelt.saksbehandler shouldBe "Z123456"

            val tilbake = service.leggTilbake(tildelt.id, tildelt.versjon, "Z123456").getOrFail()
            tilbake.versjon shouldBe 2L
            tilbake.saksbehandler shouldBe null
            repo.hent(tilbake.id) shouldBe tilbake

            val overtatt = service.ta(tilbake.id, tilbake.versjon, "Z654321").getOrFail()
            val løst = service.løs(overtatt.id, overtatt.versjon, "Z654321", InternOppgaveløsning.Forkastet).getOrFail()
            løst.versjon shouldBe 4L
            løst.løst shouldBe løst.sistEndret
            løst.opprettet shouldBe opprettet.opprettet
            repo.hent(løst.id) shouldBe løst
            repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel) shouldBe null
            repo.hentForSak(sak.id) shouldBe listOf(løst)
        }
    }

    @Test
    fun `ny hendelse bevarer eier men ugyldiggjør gammel løsning og avsluttet oppgave erstattes ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()
            val opprettet = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            val tildelt = service.ta(opprettet.id, opprettet.versjon, "Z123456").getOrFail()
            val nyttGrunnlag = endretTiltaksdeltakelseGrunnlag(
                tiltaksdeltakerId = grunnlag.tiltaksdeltakerId,
                beskrivelse = "Sluttdato er endret på nytt.",
            )
            val oppdatert = service.opprettEllerOppdater(sak.id, nyttGrunnlag).getOrFail()
            oppdatert.id shouldBe opprettet.id
            oppdatert.versjon shouldBe tildelt.versjon + 1
            oppdatert.saksbehandler shouldBe tildelt.saksbehandler
            repo.hent(oppdatert.id) shouldBe oppdatert
            oppdatert.grunnlag shouldBe nyttGrunnlag

            service.løs(tildelt.id, tildelt.versjon, "Z123456", InternOppgaveløsning.Forkastet) shouldBe
                InternOppgaveFeil.OppgavenErEndret.left()
            repo.hent(oppdatert.id) shouldBe oppdatert

            val løst = service.løs(oppdatert.id, oppdatert.versjon, "Z123456", InternOppgaveløsning.Forkastet).getOrFail()
            service.ta(løst.id, løst.versjon, "Z654321") shouldBe InternOppgaveFeil.OppgavenErLøst.left()
            service.leggTilbake(løst.id, løst.versjon, "Z123456") shouldBe InternOppgaveFeil.OppgavenErLøst.left()
            service.løs(løst.id, løst.versjon, "Z123456", InternOppgaveløsning.Forkastet) shouldBe
                InternOppgaveFeil.OppgavenErLøst.left()
            løst.oppdaterGrunnlag(grunnlag, tac.clock) shouldBe InternOppgaveFeil.OppgavenErLøst.left()

            val ny = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            ny.id shouldNotBe løst.id
            ny.versjon shouldBe 0L
            repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel) shouldBe ny
            repo.hentForSak(sak.id) shouldBe listOf(løst, ny)
            repo.hent(løst.id) shouldBe løst
        }
    }

    @ParameterizedTest
    @EnumSource(InternOppgaveløsning.Revurderingstype::class)
    fun `løser med referanse til en reell revurdering`(type: InternOppgaveløsning.Revurderingstype) {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, revurdering) = when (type) {
                InternOppgaveløsning.Revurderingstype.STANS -> iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
                InternOppgaveløsning.Revurderingstype.FORLENGELSE -> iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)
                InternOppgaveløsning.Revurderingstype.OMGJØRING -> iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            }
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val opprettet = service.opprettEllerOppdater(sak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val tildelt = service.ta(opprettet.id, opprettet.versjon, "Z123456").getOrFail()
            val løsning = InternOppgaveløsning.Revurdering(type, revurdering.id)
            val løst = service.løs(tildelt.id, tildelt.versjon, "Z123456", løsning).getOrFail()

            repo.hent(løst.id) shouldBe løst
            repo.hentForSak(sak.id).single().løsning shouldBe løsning
        }
    }

    @Test
    fun `opprettelse og endringer deltar i innsendt transaksjon`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()

            shouldThrow<IllegalStateException> {
                tac.sessionFactory.withTransactionContext { tx ->
                    val opprettet = service.opprettEllerOppdater(sak.id, grunnlag, tx).getOrFail()
                    val tildelt = service.ta(opprettet.id, opprettet.versjon, "Z123456", tx).getOrFail()
                    repo.hent(tildelt.id, tx) shouldBe tildelt
                    repo.hentForSak(sak.id, tx) shouldBe listOf(tildelt)
                    repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel, tx) shouldBe tildelt
                    error("Rull tilbake hele transaksjonen")
                }
            }
            repo.hentForSak(sak.id).shouldBeEmpty()

            tac.sessionFactory.withTransactionContext { tx ->
                service.opprettEllerOppdater(sak.id, grunnlag, tx).getOrFail()
            }
            val opprettet = repo.hentForSak(sak.id).single()
            shouldThrow<IllegalStateException> {
                tac.sessionFactory.withTransactionContext { tx ->
                    service.ta(opprettet.id, opprettet.versjon, "Z123456", tx).getOrFail()
                    error("Rull tilbake tildelingen")
                }
            }
            repo.hent(opprettet.id) shouldBe opprettet

            tac.sessionFactory.withTransactionContext { tx ->
                service.ta(opprettet.id, opprettet.versjon, "Z123456", tx).getOrFail()
            }
            val tildelt = repo.hent(opprettet.id)!!
            val nyttGrunnlag = endretTiltaksdeltakelseGrunnlag(
                tiltaksdeltakerId = grunnlag.tiltaksdeltakerId,
                beskrivelse = "Ny sluttdato i transaksjonen.",
            )
            shouldThrow<IllegalStateException> {
                tac.sessionFactory.withTransactionContext { tx ->
                    val oppdatert = service.opprettEllerOppdater(sak.id, nyttGrunnlag, tx).getOrFail()
                    repo.hent(oppdatert.id, tx) shouldBe oppdatert
                    val tilbake = service.leggTilbake(oppdatert.id, oppdatert.versjon, "Z123456", tx).getOrFail()
                    repo.hent(tilbake.id, tx) shouldBe tilbake
                    val overtatt = service.ta(tilbake.id, tilbake.versjon, "Z654321", tx).getOrFail()
                    val løst = service.løs(overtatt.id, overtatt.versjon, "Z654321", InternOppgaveløsning.Forkastet, tx).getOrFail()
                    repo.hent(løst.id, tx) shouldBe løst
                    repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel, tx) shouldBe null
                    error("Rull tilbake grunnlag, eierskifte og løsning")
                }
            }
            repo.hent(tildelt.id) shouldBe tildelt
            repo.hentÅpen(sak.id, grunnlag.type, grunnlag.nøkkel) shouldBe tildelt
        }
    }
}
