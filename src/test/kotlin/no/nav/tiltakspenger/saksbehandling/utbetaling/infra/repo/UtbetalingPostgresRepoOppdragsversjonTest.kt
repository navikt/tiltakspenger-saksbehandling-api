package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * Tester for hvordan [UtbetalingPostgresRepo] skiller på [Oppdragsversjon].
 */
class UtbetalingPostgresRepoOppdragsversjonTest {

    @Test
    fun `henting av oppdragsversjon kaster når saken ikke finnes`() {
        withTestApplicationContextAndPostgres { tac ->
            shouldThrow<IllegalStateException> {
                tac.utbetalingContext.utbetalingRepo.hentOppdragsversjon(SakId.random())
            }
        }
    }

    /**
     * Status fra Hel Ved på Kafka skal bare lagres på saker som bruker [Oppdragsversjon.V3].
     * Saker på [Oppdragsversjon.V2] får status fra statusjobben mot utsjekk.
     */
    @Test
    fun `kvitteringsstatus lagres ikke på saker som bruker oppdragsversjon 2 eller på utbetalinger som ikke finnes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                jobber = JobberEtterIverksettelse.ingen,
            )!!
            val repo = tac.utbetalingContext.utbetalingRepo

            repo.hentOppdragsversjon(sak.id) shouldBe Oppdragsversjon.V2
            repo.lagreKvitteringsstatus(vedtak.utbetaling.id, Utbetalingsstatus.Ok) shouldBe
                KvitteringsstatusLagring.FantIkkeUtbetaling
            repo.lagreKvitteringsstatus(UtbetalingId.random(), Utbetalingsstatus.Ok) shouldBe
                KvitteringsstatusLagring.FantIkkeUtbetaling
        }
    }
}
