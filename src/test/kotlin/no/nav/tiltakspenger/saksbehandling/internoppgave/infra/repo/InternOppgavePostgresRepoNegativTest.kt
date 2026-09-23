package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import arrow.core.left
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgave
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveFeil
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveService
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.time.Clock
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Negative tester omgår servicen for å prøve utdaterte skrivinger og databasens egne invarianter.
 * Direkte SQL brukes bare til feiltilstander som domenet ikke kan produsere.
 * Gyldig utgangstilstand bygges gjennom routes og InternOppgaveService, modulens foreløpige inngang.
 */
class InternOppgavePostgresRepoNegativTest {

    @Test
    fun `unik indeks stopper duplikat id og flere åpne oppgaver for samme deltaker på en sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()
            val oppgave = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()

            repo.opprett(oppgave) shouldBe false
            repo.opprett(InternOppgave.opprett(sak.id, grunnlag, tac.clock)) shouldBe false
            repo.hentForSak(sak.id) shouldBe listOf(oppgave)

            val feil = shouldThrow<PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            """
                            INSERT INTO intern_oppgave
                                (id, sak_id, type, nøkkel, grunnlag, opprettet, sist_endret, versjon, dialog)
                            SELECT :ny_id, sak_id, type, nøkkel, grunnlag, opprettet, sist_endret, versjon, dialog
                            FROM intern_oppgave WHERE id = :id
                            """.trimIndent(),
                            mapOf("ny_id" to InternOppgaveId.random().toString(), "id" to oppgave.id.toString()),
                        ).asUpdate,
                    )
                }
            }
            feil.sqlState shouldBe "23505"
            feil.message shouldContain "intern_oppgave_en_åpen_per_nøkkel"

            val (annenSak) = opprettSakOgSøknad(tac)
            val annenOppgave = service.opprettEllerOppdater(annenSak.id, grunnlag).getOrFail()
            repo.hentForSak(annenSak.id) shouldBe listOf(annenOppgave)
            repo.hentForSak(sak.id) shouldBe listOf(oppgave)
        }
    }

    @Test
    fun `CAS avviser gammel versjon lukket oppgave feil versjonssprang og slettet rad`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()
            val oppgave = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            val gammelTildeling = oppgave.ta(annenTestsaksbehandler, tac.clock).getOrFail()
            val tildelt = service.ta(oppgave.id, oppgave.versjon, testsaksbehandler).getOrFail()

            repo.oppdater(gammelTildeling, oppgave.versjon) shouldBe false
            repo.hent(oppgave.id) shouldBe tildelt
            shouldThrow<IllegalArgumentException> { repo.oppdater(tildelt, tildelt.versjon) }

            val løst = service.løs(tildelt.id, tildelt.versjon, testsaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse).getOrFail()
            val forsøkPåGjenåpning = tildelt.oppdaterGrunnlag(grunnlag, tac.clock).getOrFail()
                .oppdaterGrunnlag(grunnlag, tac.clock).getOrFail()
            repo.oppdater(forsøkPåGjenåpning, løst.versjon) shouldBe false
            repo.hent(løst.id) shouldBe løst

            val ny = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            val nyTildeling = ny.ta(testsaksbehandler, tac.clock).getOrFail()
            tac.sessionFactory.withSession { session ->
                session.run(queryOf("DELETE FROM intern_oppgave WHERE id = :id", mapOf("id" to ny.id.toString())).asUpdate)
            }
            repo.oppdater(nyTildeling, ny.versjon) shouldBe false
            repo.hent(ny.id) shouldBe null
            service.ta(ny.id, ny.versjon, testsaksbehandler) shouldBe InternOppgaveFeil.FantIkkeOppgave.left()
        }
    }

    @Test
    fun `fremmednøkler avviser ukjent sak og ukjent behandling uten å endre oppgaven`() {
        withTestApplicationContextAndPostgres { tac ->
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()
            val ukjentSak = shouldThrow<PSQLException> {
                service.opprettEllerOppdater(SakId.random(), grunnlag)
            }
            ukjentSak.sqlState shouldBe "23503"
            ukjentSak.message shouldContain "intern_oppgave_sak_id_fkey"

            val (sak) = opprettSakOgSøknad(tac)
            val oppgave = service.opprettEllerOppdater(sak.id, grunnlag).getOrFail()
            val tildelt = service.ta(oppgave.id, oppgave.versjon, testsaksbehandler).getOrFail()
            val ukjentBehandling = shouldThrow<PSQLException> {
                service.løs(
                    tildelt.id,
                    tildelt.versjon,
                    testsaksbehandler,
                    InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.STANS, RammebehandlingId.random()),
                    testbegrunnelse,
                )
            }
            ukjentBehandling.sqlState shouldBe "23503"
            ukjentBehandling.message shouldContain "intern_oppgave_behandling_id_fkey"
            repo.hent(tildelt.id) shouldBe tildelt
        }
    }

    @Test
    fun `databasen krever konsistent løsning begrunnelse saksbehandler behandling tidspunkt og dialog`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val service = InternOppgaveService(repo, tac.clock)
            val oppgave = service.opprettEllerOppdater(sak.id, endretTiltaksdeltakelseGrunnlag()).getOrFail()
            val ugyldigeEndringer = listOf(
                "løsning = 'FORKASTET'" to "intern_oppgave_løsning",
                "løst = sist_endret, begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "behandling_id = :behandling_id" to "intern_oppgave_løsning",
                "løsning = 'FORKASTET', løst = sist_endret, begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'STANS', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'FORLENGELSE', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'OMGJORING', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'FORKASTET', løst = sist_endret, saksbehandler = 'Z123456', behandling_id = :behandling_id, begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'UKJENT', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "løsning = 'FORKASTET', løst = sist_endret + interval '1 second', saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_løsning",
                "begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT'" to "intern_oppgave_begrunnelse",
                "begrunnelse_fritekst = 'Fritekst'" to "intern_oppgave_begrunnelse",
                "løsning = 'FORKASTET', løst = sist_endret, saksbehandler = 'Z123456'" to "intern_oppgave_begrunnelse",
                "løsning = 'FORKASTET', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse = 'ENDRINGEN_ER_ALLEREDE_HANDTERT', begrunnelse_fritekst = 'Fritekst'" to "intern_oppgave_begrunnelse",
                "løsning = 'FORKASTET', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse_fritekst = ''" to "intern_oppgave_begrunnelse",
                "løsning = 'FORKASTET', løst = sist_endret, saksbehandler = 'Z123456', begrunnelse_fritekst = '  '" to "intern_oppgave_begrunnelse",
                "dialog = '{}'::jsonb" to "intern_oppgave_dialog_check",
                "sist_endret = opprettet - interval '1 second'" to "intern_oppgave_tid",
                "saksbehandler = '   '" to "intern_oppgave_saksbehandler_check",
                "versjon = -1" to "intern_oppgave_versjon_check",
            )
            ugyldigeEndringer.forEach { (endring, constraint) ->
                val feil = shouldThrow<PSQLException> {
                    tac.sessionFactory.withSession { session ->
                        session.run(
                            queryOf(
                                "UPDATE intern_oppgave SET $endring WHERE id = :id",
                                mapOf("id" to oppgave.id.toString(), "behandling_id" to revurdering.id.toString()),
                            ).asUpdate,
                        )
                    }
                }
                feil.sqlState shouldBe "23514"
                feil.message shouldContain constraint
                repo.hent(oppgave.id) shouldBe oppgave
            }
        }
    }

    @Test
    fun `to samtidige opprettelser og tildelinger har bare en vinner`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            val repo = InternOppgavePostgresRepo(tac.sessionFactory)
            val grunnlag = endretTiltaksdeltakelseGrunnlag()
            val clock = Clock.fixed(tac.clock.instant(), tac.clock.zone)
            val beggeHarLest = CountDownLatch(2)
            val opprettelsesrepo = object : InternOppgaveRepo by repo {
                override fun hentÅpen(
                    sakId: SakId,
                    type: InternOppgavetype,
                    nøkkel: String,
                    sessionContext: SessionContext?,
                ): InternOppgave? = repo.hentÅpen(sakId, type, nøkkel, sessionContext).also {
                    beggeHarLest.countDown()
                    check(beggeHarLest.await(10, TimeUnit.SECONDS))
                }
            }
            val service = InternOppgaveService(opprettelsesrepo, clock)
            val opprettelser = samtidig(
                { service.opprettEllerOppdater(sak.id, grunnlag) },
                { service.opprettEllerOppdater(sak.id, grunnlag) },
            )
            opprettelser.count { it.isRight() } shouldBe 1
            opprettelser.single { it.isLeft() } shouldBe InternOppgaveFeil.OppgavenErEndret.left()
            val oppgave = repo.hentForSak(sak.id).single()

            val beggeHarLestOppgaven = CountDownLatch(2)
            val tildelingsrepo = object : InternOppgaveRepo by repo {
                override fun hent(id: InternOppgaveId, sessionContext: SessionContext?): InternOppgave? =
                    repo.hent(id, sessionContext).also {
                        beggeHarLestOppgaven.countDown()
                        check(beggeHarLestOppgaven.await(10, TimeUnit.SECONDS))
                    }
            }
            val tildelingsservice = InternOppgaveService(tildelingsrepo, clock)
            val tildelinger = samtidig(
                { tildelingsservice.ta(oppgave.id, oppgave.versjon, testsaksbehandler) },
                { tildelingsservice.ta(oppgave.id, oppgave.versjon, annenTestsaksbehandler) },
            )
            tildelinger.count { it.isRight() } shouldBe 1
            tildelinger.single { it.isLeft() } shouldBe InternOppgaveFeil.OppgavenErEndret.left()
            repo.hent(oppgave.id) shouldBe tildelinger.single { it.isRight() }.getOrFail()
        }
    }

    private fun <T> samtidig(første: () -> T, andre: () -> T): List<T> {
        val executor = Executors.newFixedThreadPool(2)
        return try {
            val førsteResultat = executor.submit(Callable { første() })
            val andreResultat = executor.submit(Callable { andre() })
            listOf(førsteResultat.get(20, TimeUnit.SECONDS), andreResultat.get(20, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }
}
