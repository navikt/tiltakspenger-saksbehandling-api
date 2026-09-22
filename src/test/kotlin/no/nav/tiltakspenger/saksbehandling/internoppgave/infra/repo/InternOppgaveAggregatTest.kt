package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgave
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveService
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration

/**
 * Leser hele den isolerte køen på tvers av saker uten filter.
 * Sakene bygges gjennom routes, oppgavene gjennom modulens foreløpige inngang InternOppgaveService.
 */
class InternOppgaveAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `uløste inkluderer tildelte sorterer eldst og id først og støtter paging`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (førsteSak) = opprettSakOgSøknad(tac)
            val (andreSak) = opprettSakOgSøknad(tac)
            val (tredjeSak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val tidspunkt = Clock.fixed(tac.clock.instant(), tac.clock.zone)
            val service = InternOppgaveService(repo, tidspunkt)
            val senereService = InternOppgaveService(repo, Clock.offset(tidspunkt, Duration.ofSeconds(1)))
            repo.hentUløste(limit = 100, offset = 0).shouldBeEmpty()

            // Nyeste oppgave skrives først for å skille tidsrekkefølge fra innsettingsrekkefølge.
            val nyest = senereService.opprettEllerOppdater(tredjeSak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val første = service.opprettEllerOppdater(førsteSak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val andre = service.opprettEllerOppdater(andreSak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val tildelt = service.ta(andre.id, andre.versjon, "Z123456").getOrFail()
            val forkastes = service.opprettEllerOppdater(tredjeSak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val tatt = service.ta(forkastes.id, forkastes.versjon, "Z123456").getOrFail()
            service.løs(tatt.id, tatt.versjon, "Z123456", InternOppgaveløsning.Forkastet).getOrFail()

            første.opprettet shouldBe andre.opprettet
            val forventet = listOf(første, tildelt).sortedBy { it.id.toString() } + nyest
            repo.hentUløste(limit = 100, offset = 0) shouldBe forventet
            repo.hentUløste(limit = 1, offset = 0) shouldBe forventet.take(1)
            repo.hentUløste(limit = 1, offset = 1) shouldBe forventet.drop(1).take(1)
            repo.hentUløste(limit = 2, offset = 1) shouldBe forventet.drop(1)
            repo.hentUløste(limit = 1, offset = 3).shouldBeEmpty()

            val avslutningsservice = InternOppgaveService(repo, Clock.offset(tidspunkt, Duration.ofSeconds(2)))
            forventet.forEach { oppgave ->
                val eiet: InternOppgave = if (oppgave.saksbehandler == null) {
                    avslutningsservice.ta(oppgave.id, oppgave.versjon, "Z123456").getOrFail()
                } else {
                    oppgave
                }
                avslutningsservice.løs(eiet.id, eiet.versjon, "Z123456", InternOppgaveløsning.Forkastet).getOrFail()
            }
            repo.hentUløste(limit = 100, offset = 0).shouldBeEmpty()

            listOf(-1, 0, 101).forEach { ugyldigLimit ->
                shouldThrow<IllegalArgumentException> { repo.hentUløste(limit = ugyldigLimit, offset = 0) }
            }
            shouldThrow<IllegalArgumentException> { repo.hentUløste(limit = 1, offset = -1) }
        }
    }
}
