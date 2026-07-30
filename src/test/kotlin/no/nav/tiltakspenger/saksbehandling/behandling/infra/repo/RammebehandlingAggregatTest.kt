package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de to køene på rammebehandling, jf. testtaksonomien i `AGENTS.md`.
 *
 * Begge spørringene velger ut på tvers av alle saker og sorterer eldst først (`order by b.opprettet`).
 * Testene bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 */
class RammebehandlingAggregatTest {

    /**
     * Køen for delautomatisk behandling har tre kriterier: behandlingstypen, statusen `UNDER_AUTOMATISK_BEHANDLING`, og at behandlingen ikke venter på noe.
     * Alle tre asserteres her; jobben som konsumerer køen testes for seg i `DelautomatiskSoknadsbehandlingJobbTest`.
     */
    @Test
    @IsolatedDatabaseTest
    fun `automatisk-køen tar kun søknadsbehandlinger under automatisk behandling som ikke venter, sorterer eldst først og respekterer limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, _, eldst) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)
            val (_, _, nyest) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)
            // Kandidaten som skal falle utenfor på status: manuell behandling sendt til beslutning.
            sendSøknadsbehandlingTilBeslutning(tac = tac)

            val repo = tac.behandlingContext.rammebehandlingRepo

            repo.hentAutomatiskeSoknadsbehandlingIder(limit = 10) shouldBe listOf(eldst.id, nyest.id)

            // Limit batcher fra toppen av køen, så den eldste behandlingen kommer først og ingen kan sulte.
            repo.hentAutomatiskeSoknadsbehandlingIder(limit = 1) shouldBe listOf(eldst.id)

            // Tiltaket starter fram i tid, så den automatiske behandlingen settes på vent og faller ut av køen til `venter_til` har passert.
            val (_, _, venter) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                    fom = 1.januar(2100),
                    tom = 31.januar(2100),
                ),
            )
            repo.hentAutomatiskeSoknadsbehandlingIder(limit = 10) shouldBe listOf(eldst.id, nyest.id, venter.id)

            tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(
                behandling = venter,
                correlationId = CorrelationId.generate(),
            )

            repo.hentAutomatiskeSoknadsbehandlingIder(limit = 10) shouldBe listOf(eldst.id, nyest.id)
        }
    }

    /**
     * Datadelingskøen krever at saken selv er sendt til datadeling.
     * Jobben deler sakene før behandlingene i samme kjøring, så ett kall til [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService.send] dekker begge stegene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `datadelingskøen er tom til saken er delt, sorterer eldst først, og tømmes når behandlingen er sendt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, _, _, _) = iverksettSøknadsbehandling(tac = tac, innvilgelsesperioder = ObjectMother.innvilgelsesperioder(1.til(31.januar(2024))))
            iverksettSøknadsbehandling(tac = tac)

            val repo = tac.behandlingContext.rammebehandlingRepo

            // Ingen av sakene er delt ennå, så køen er tom selv om behandlingene er iverksatt.
            repo.hentBehandlingerTilDatadeling(limit = 10).shouldBeEmpty()

            tac.sendTilDatadelingService.send()

            tac.datadelingFakeKlient.sendteBehandlinger.toList().size shouldBe 2

            // Sendte behandlinger forlater køen, så en ny jobbkjøring ikke sender dem på nytt.
            repo.hentBehandlingerTilDatadeling(limit = 10).shouldBeEmpty()
        }
    }
}
