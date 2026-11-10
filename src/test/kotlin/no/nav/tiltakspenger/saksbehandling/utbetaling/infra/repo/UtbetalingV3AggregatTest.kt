package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Aggregat-test for V3-tellerne, med utbetalinger bygget gjennom route-lagets prodstier. */
class UtbetalingV3AggregatTest {
    @Test
    @IsolatedDatabaseTest
    fun `V3-tellerne skiller mellom feil og uferdige utbetalinger og bruker tre dagers grense`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (usendtSak, usendt) = sakMedUtbetaling(tac)
            val (_, ikkePåbegynt) = sakMedUtbetaling(tac)
            val (_, sendtTilOppdrag) = sakMedUtbetaling(tac)
            val (_, ok) = sakMedUtbetaling(tac)
            val (_, okUtenUtbetaling) = sakMedUtbetaling(tac)
            val (_, avbrutt) = sakMedUtbetaling(tac)
            val (_, feilet) = sakMedUtbetaling(tac)
            val (_, korrigertFeilet) = sakMedUtbetaling(tac)
            val (v2Sak, v2Utbetaling) = sakMedUtbetaling(tac)
            val (_, nyere) = sakMedUtbetaling(tac)

            val repo = tac.utbetalingContext.utbetalingRepo
            repo.lagreKvitteringsstatus(ikkePåbegynt, Utbetalingsstatus.IkkePåbegynt) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(sendtTilOppdrag, Utbetalingsstatus.SendtTilOppdrag) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(ok, Utbetalingsstatus.Ok) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(okUtenUtbetaling, Utbetalingsstatus.OkUtenUtbetaling) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(avbrutt, Utbetalingsstatus.Avbrutt) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(feilet, Utbetalingsstatus.FeiletMotOppdrag) shouldBe KvitteringsstatusLagring.Lagret
            repo.lagreKvitteringsstatus(korrigertFeilet, Utbetalingsstatus.FeiletMotOppdrag) shouldBe KvitteringsstatusLagring.Lagret

            repo.lagreKvitteringsstatus(ok, Utbetalingsstatus.SendtTilOppdrag) shouldBe
                KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal
            repo.lagreKvitteringsstatus(feilet, Utbetalingsstatus.SendtTilOppdrag) shouldBe
                KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal
            repo.lagreKvitteringsstatus(korrigertFeilet, Utbetalingsstatus.Ok) shouldBe KvitteringsstatusLagring.Lagret

            val terminalstatus = tac.sessionFactory.withTransactionContext { context ->
                listOf(ok, feilet, korrigertFeilet).mapNotNull { repo.hent(it, context)?.status }
            }
            terminalstatus shouldBe listOf(Utbetalingsstatus.Ok, Utbetalingsstatus.FeiletMotOppdrag, Utbetalingsstatus.Ok)

            val eldreEnn = nå(tac.clock).minusDays(3)
            listOf(
                usendt,
                ikkePåbegynt,
                sendtTilOppdrag,
                ok,
                okUtenUtbetaling,
                avbrutt,
                feilet,
                v2Utbetaling,
            ).forEach {
                settOpprettet(tac, it, eldreEnn)
            }
            settOpprettet(tac, nyere, eldreEnn.plus(1, ChronoUnit.MICROS))
            settOppdragsversjon(tac, v2Sak, Oppdragsversjon.V2)

            repo.antallUtbetalingerUtenOkPåVersjon3(eldreEnn) shouldBe 3L
            repo.antallUtbetalingerUtenOkPåVersjon3(eldreEnn.minus(1, ChronoUnit.MICROS)) shouldBe 0L
            repo.antallFeiledeUtbetalingerPåVersjon3() shouldBe 1L

            val observertFeil = tac.sessionFactory.withTransactionContext { context ->
                repo.hent(feilet, context)
            }
            observertFeil?.status shouldBe Utbetalingsstatus.FeiletMotOppdrag
            repo.hentOppdragsversjon(usendtSak) shouldBe Oppdragsversjon.V3
        }
    }

    private suspend fun ApplicationTestBuilder.sakMedUtbetaling(
        tac: TestApplicationContext,
    ): Pair<SakId, UtbetalingId> {
        val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
        val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse.ingen,
        )!!
        return sak.id to vedtak.utbetaling.id
    }

    private fun settOpprettet(tac: TestApplicationContext, id: UtbetalingId, opprettet: LocalDateTime) {
        (tac.sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                sqlQuery(
                    """
                    update utbetaling
                    set opprettet = :opprettet
                    where id = :id
                    """,
                    "id" to id.toString(),
                    "opprettet" to opprettet,
                ).asUpdate,
            )
        }
    }

    private fun settOppdragsversjon(tac: TestApplicationContext, sakId: SakId, versjon: Oppdragsversjon) {
        (tac.sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                sqlQuery(
                    """
                    update sak
                    set oppdragsversjon = :versjon
                    where id = :sak_id
                    """,
                    "sak_id" to sakId.toString(),
                    "versjon" to versjon.tilDb(),
                ).asUpdate,
            )
        }
    }
}
