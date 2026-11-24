package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class EksternOppgavePostgresRepoTest {
    @Test
    fun `lagrer oppgaven én gang, og henter oppgavene for saken sortert på tidspunkt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac, fnr = ObjectMother.gyldigFnr())
            val (annenSak) = opprettSakOgSøknad(tac = tac, fnr = ObjectMother.gyldigFnr())
            val repo = tac.eksternOppgaveRepo
            val tidspunkt = nå(tac.clock).withNano(0)
            val senest = oppgave(sak.id, tidspunkt, tilleggstekst = null)
            val tidligst = oppgave(sak.id, tidspunkt.minusDays(1), tilleggstekst = "Endret tiltaksdeltakelse")
            val duplikat = senest.copy(opprettet = tidspunkt.plusDays(1), tilleggstekst = "Duplikat")

            repo.hentForSakId(sak.id).shouldBeEmpty()
            repo.lagre(senest)
            repo.lagre(tidligst)
            repo.lagre(duplikat)
            repo.lagre(oppgave(annenSak.id, tidspunkt.minusDays(2), tilleggstekst = null))

            val lagret = repo.hentForSakId(sak.id)
            lagret.map { Triple(it.oppgaveId, it.opprettet, it.tilleggstekst) } shouldBe listOf(
                Triple(tidligst.oppgaveId, tidligst.opprettet, "Endret tiltaksdeltakelse"),
                Triple(senest.oppgaveId, senest.opprettet, null),
            )
            lagret.map { it.sakId }.distinct() shouldBe listOf(sak.id)
            // Grunnlaget leses som rå json, og jsonb normaliserer rekkefølge og mellomrom.
            lagret.last().grunnlag shouldEqualJson senest.grunnlag.toDbJson()
        }
    }

    private fun oppgave(sakId: SakId, opprettet: LocalDateTime, tilleggstekst: String?) = EksternOppgave(
        oppgaveId = OppgaveId(UUID.randomUUID().toString()),
        sakId = sakId,
        opprettet = opprettet,
        grunnlag = Oppgavegrunnlag.EndretTiltaksdeltakelse(
            kilde = Kilde.Tiltakshistorikk(opprettet),
            verdi = ObjectMother.tiltaksdeltakelseTac(fom = 17.mars(2026), tom = 30.juni(2026)).tilLibsDeltakelse(),
        ),
        tilleggstekst = tilleggstekst,
    )
}
