package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for køen av brukers meldekort som skal behandles automatisk, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker, så testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Merk at `order by` her finnes for `distinct on (mk.sak_id)`, ikke for å gi en rekkefølge på tvers av saker.
 * Rekkefølgen *innenfor* en sak er kontrakten — eldste kjede først — mens rekkefølgen mellom saker er et biprodukt, og asserteres derfor ikke.
 */
class BrukersMeldekortAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `køen tar ett meldekort per sak, venter på at forrige utbetaling er gjort opp, og rykker fram til neste kjede`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (aFørste, aAndre) = sakMedMeldekortPåToKjeder(tac)
            val (bFørste, bAndre) = sakMedMeldekortPåToKjeder(tac)

            val repo = tac.meldekortContext.brukersMeldekortRepo

            // `distinct on (sak_id)`: én rad per sak, og det er den eldste kjeden som står for tur.
            repo.hentMeldekortSomSkalBehandlesAutomatisk(limit = 10).map { it.id } shouldContainExactlyInAnyOrder
                listOf(aFørste, bFørste)

            // Limit batcher køen, så en jobbkjøring kan ta en håndfull saker av gangen.
            repo.hentMeldekortSomSkalBehandlesAutomatisk(limit = 1).size shouldBe 1

            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Behandlingen lager en utbetaling som ennå ikke er gjort opp, og da holdes hele saken utenfor køen.
            // Det er dette som hindrer at meldekort nummer to behandles før det første er utbetalt.
            repo.hentMeldekortSomSkalBehandlesAutomatisk(limit = 10).shouldBeEmpty()

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            // Med utbetalingen kvittert ut rykker køen fram til neste kjede på begge sakene.
            repo.hentMeldekortSomSkalBehandlesAutomatisk(limit = 10).map { it.id } shouldContainExactlyInAnyOrder
                listOf(aAndre, bAndre)
        }
    }

    /**
     * Bygger en sak med brukers meldekort på de to første meldeperiodekjedene, gjennom route-laget.
     *
     * @return id-ene til meldekortet på første og andre kjede, i den rekkefølgen.
     */
    private suspend fun ApplicationTestBuilder.sakMedMeldekortPåToKjeder(
        tac: TestApplicationContext,
    ): Pair<MeldekortId, MeldekortId> {
        val (sak) = iverksettSøknadsbehandling(tac = tac)
        val meldekortIder = (0..1).map { kjedeIndeks ->
            val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
            val (_, brukersMeldekort) = mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = meldeperiode.id,
                sakId = sak.id,
                dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
            )
            brukersMeldekort!!.id
        }
        return meldekortIder[0] to meldekortIder[1]
    }
}
