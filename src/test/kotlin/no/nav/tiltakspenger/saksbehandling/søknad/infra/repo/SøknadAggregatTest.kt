package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for køen av ubehandlede søknader, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker, så testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 * Den sorterer eldst først (`order by soknad.opprettet`), og testen asserter den rekkefølgen.
 *
 * To av utvalgskriteriene er ikke dekket her, fordi de ikke lar seg nå gjennom eksisterende route-byggere:
 * `soknad.avbrutt is null` krever en inngang for å avbryte en søknad uten behandling, og `soknadstype = DIGITAL` krever en papirsøknad.
 * Begge kommer inn her når byggerne finnes.
 */
class SøknadAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `køen tar kun søknader uten behandling, sorterer eldst først, respekterer limit og tømmes når behandlingen opprettes`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, eldst) = opprettSakOgSøknad(tac = tac)
            val (_, nyest) = opprettSakOgSøknad(tac = tac)

            val repo = tac.søknadContext.søknadRepo

            repo.hentUbehandledeSøknadIder(limit = 10) shouldBe listOf(eldst.id, nyest.id)

            // Limit batcher fra toppen av køen, så den eldste søknaden kommer først og ingen kan sulte.
            repo.hentUbehandledeSøknadIder(limit = 1) shouldBe listOf(eldst.id)

            tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                soknad = eldst as InnvilgbarSøknad,
                correlationId = CorrelationId.generate(),
            ).getOrFail()

            // Søknaden forlater køen så snart den har fått en behandling.
            repo.hentUbehandledeSøknadIder(limit = 10) shouldBe listOf(nyest.id)

            tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                soknad = nyest as InnvilgbarSøknad,
                correlationId = CorrelationId.generate(),
            ).getOrFail()

            repo.hentUbehandledeSøknadIder(limit = 10).shouldBeEmpty()
        }
    }
}
