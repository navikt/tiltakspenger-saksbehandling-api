package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.jobb

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.time.Duration.Companion.hours

class HentArenaStatistikkJobbTest {
    private fun statistikk(måned: String, sakerMedRett: Int) = ArenaStatistikkForMåned(
        måned = YearMonth.parse(måned),
        sakerMedRett = sakerMedRett,
        nyeRettigheter = 1,
        gjenopptak = 2,
        endringer = 3,
        stans = 4,
    )

    private fun TestApplicationContextMedPostgres.lagrede(): List<Pair<String, Int>> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery("select maned, saker_med_rett from arena_statistikk_per_maned order by maned").map {
                YearMonth.from(it.localDate("maned")).toString() to it.int("saker_med_rett")
            }.asList,
        )
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter i år og i fjor, venter et døgn og skriver over månedene`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            tac.leggTilStatistikkFraArena(2024, listOf(statistikk("2024-12", 10)))
            tac.leggTilStatistikkFraArena(2025, listOf(statistikk("2025-01", 20)))

            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.Ferdig
            tac.lagrede() shouldBe listOf("2024-12" to 10, "2025-01" to 20)

            tac.leggTilStatistikkFraArena(2025, listOf(statistikk("2025-01", 21), statistikk("2025-02", 30)))
            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.IngenArbeid

            tac.clock.spolFrem(25.hours)
            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.Ferdig
            tac.lagrede() shouldBe listOf("2024-12" to 10, "2025-01" to 21, "2025-02" to 30)
        }
    }
}
