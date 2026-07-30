package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.KlageinstansKlagehendelseConsumer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Aggregat-test for køen av klageinstanshendelser som ennå ikke er knyttet til en sak, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker (`where sak_id is null`) og sorterer eldst først (`order by opprettet`).
 * Testen bygger flere hendelser og asserter hele køen, uten å filtrere.
 *
 * Hendelsene kommer inn gjennom prodstien, altså Kabal-consumeren.
 * At `sak_id` er null når hendelsen lagres er poenget: consumeren tar imot og lagrer, mens jobben gjør koblingsarbeidet etterpå.
 */
class KlagehendelseAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `køen tar hendelser uten sak, sorterer eldst først, respekterer limit og tømmes når hendelsen knyttes til en klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = mottaKlageinstanshendelse(tac)
            val nyest = mottaKlageinstanshendelse(tac)

            val repo = tac.klagebehandlingContext.klagehendelseRepo

            repo.hentUbehandledeHendelseIder(limit = 10) shouldBe listOf(eldst, nyest)

            // Limit batcher fra toppen av køen, så den eldste hendelsen kommer først og ingen kan sulte.
            repo.hentUbehandledeHendelseIder(limit = 1) shouldBe listOf(eldst)

            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelse(eldst)

            // Hendelsen forlater køen så snart den har fått en sak.
            repo.hentUbehandledeHendelseIder(limit = 10) shouldBe listOf(nyest)

            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelse(nyest)

            repo.hentUbehandledeHendelseIder(limit = 10).shouldBeEmpty()
        }
    }

    /** Oppretter en opprettholdt klagebehandling og lar Kabal-consumeren ta imot en avsluttet-hendelse for den. */
    private suspend fun ApplicationTestBuilder.mottaKlageinstanshendelse(
        tac: TestApplicationContext,
    ): KlagehendelseId {
        val (_, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
        return KlageinstansKlagehendelseConsumer.consume(
            key = "some-unused-uuid",
            value = GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                utfall = KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                kildeReferanse = klagebehandling.id.toString(),
            ),
            clock = tac.clock,
            lagreNyHendelse = tac.klagebehandlingContext.klagehendelseRepo::lagreNyHendelse,
        )!!
    }
}
