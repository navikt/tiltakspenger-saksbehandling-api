package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.jobb

import arrow.core.left
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.arenavedtak.ArenaStatistikkRepo
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForSisteDagMedRett
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.uventetStatus
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
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

    private fun TestApplicationContextMedPostgres.lagretSisteDagMedRett(): List<ArenaStatistikkForSisteDagMedRett> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery("select etter, siste_dag_med_rett, saker, personer from arena_siste_dag_med_rett order by siste_dag_med_rett nulls last").map {
                it.localDate("etter") shouldBe ArenaSisteDagMedRettEtter.startdato
                ArenaStatistikkForSisteDagMedRett(it.localDateOrNull("siste_dag_med_rett"), it.int("saker"), it.int("personer"))
            }.asList,
        )
    }

    // Spørringen John bruker mot tabellen, kjørt her så den er prøvd mot ekte Postgres.
    private fun TestApplicationContextMedPostgres.rettEtterDatoOgSluttmåned(): List<Triple<String, Int, Int>> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery(
                """
                SELECT 'etter ' || x.dato AS hva, sum(saker) AS saker, sum(personer) AS personer
                FROM arena_siste_dag_med_rett
                CROSS JOIN (VALUES (date '2026-11-30'), (date '2026-12-15')) AS x(dato)
                WHERE siste_dag_med_rett IS NULL OR siste_dag_med_rett > x.dato
                GROUP BY x.dato
                UNION ALL
                SELECT 'sluttmåned ' || coalesce(to_char(date_trunc('month', siste_dag_med_rett), 'YYYY-MM'), 'åpen'), sum(saker), sum(personer)
                FROM arena_siste_dag_med_rett
                WHERE siste_dag_med_rett IS NULL OR siste_dag_med_rett > date '2026-11-30'
                GROUP BY date_trunc('month', siste_dag_med_rett)
                ORDER BY 1
                """.trimIndent(),
            ).map { Triple(it.string("hva"), it.int("saker"), it.int("personer")) }.asList,
        )
    }

    private fun utbetalinger(måned: String, beløpGrunnsats: String) = ArenaUtbetalingerForMåned(
        måned = YearMonth.parse(måned),
        personerMedUtbetaling = 2,
        dagerUtbetalt = BigDecimal("20.00"),
        beløpGrunnsats = BigDecimal(beløpGrunnsats),
        beløpBarnetillegg = BigDecimal("106.00"),
        personerMedBeregnetIkkeUtbetalt = 1,
        beløpBeregnetIkkeUtbetalt = BigDecimal("1425.00"),
    )

    private fun TestApplicationContextMedPostgres.lagredeUtbetalinger(): List<ArenaUtbetalingerForMåned> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery("select * from arena_utbetalinger_per_maned order by maned").map {
                ArenaUtbetalingerForMåned(
                    måned = YearMonth.from(it.localDate("maned")),
                    personerMedUtbetaling = it.int("personer_med_utbetaling"),
                    dagerUtbetalt = it.bigDecimal("dager_utbetalt"),
                    beløpGrunnsats = it.bigDecimal("belop_grunnsats"),
                    beløpBarnetillegg = it.bigDecimal("belop_barnetillegg"),
                    personerMedBeregnetIkkeUtbetalt = it.int("personer_med_beregnet_ikke_utbetalt"),
                    beløpBeregnetIkkeUtbetalt = it.bigDecimal("belop_beregnet_ikke_utbetalt"),
                )
            }.asList,
        )
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter fra 2024 til og med neste år, venter et døgn og skriver over månedene`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(1.januar(2026))),
            runIsolated = true,
        ) { tac ->
            tac.leggTilStatistikkFraArena(2024, listOf(statistikk("2024-12", 10)))
            tac.leggTilStatistikkFraArena(2025, listOf(statistikk("2025-01", 20)))
            tac.leggTilStatistikkFraArena(2026, listOf(statistikk("2026-01", 40)))
            tac.leggTilStatistikkFraArena(2027, listOf(statistikk("2027-01", 50)))

            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.Ferdig
            tac.lagrede() shouldBe listOf("2024-12" to 10, "2025-01" to 20, "2026-01" to 40, "2027-01" to 50)

            tac.leggTilStatistikkFraArena(2025, listOf(statistikk("2025-01", 21), statistikk("2025-02", 30)))
            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.IngenArbeid

            tac.clock.spolFrem(25.hours)
            tac.hentArenaStatistikkJobb.hentOgLagre() shouldBe TaskResultat.Ferdig
            tac.lagrede() shouldBe listOf("2024-12" to 10, "2025-01" to 21, "2025-02" to 30, "2026-01" to 40, "2027-01" to 50)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter fordelingen på siste dag med rett, venter et døgn og erstatter hele fordelingen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val første = listOf(
                ArenaStatistikkForSisteDagMedRett(LocalDate.of(2026, 11, 30), 5, 4),
                ArenaStatistikkForSisteDagMedRett(LocalDate.of(2026, 12, 31), 3, 2),
                ArenaStatistikkForSisteDagMedRett(null, 2, 2),
            )
            tac.settSisteDagMedRettFraArena(ArenaSisteDagMedRettEtter(ArenaSisteDagMedRettEtter.startdato, første))

            tac.hentArenaStatistikkJobb.hentOgLagreSisteDagMedRett() shouldBe TaskResultat.Ferdig
            tac.lagretSisteDagMedRett() shouldBe første

            // 30.11.2026 er borte og 31.01.2027 er ny, så gamle rader må slettes.
            val andre = listOf(
                ArenaStatistikkForSisteDagMedRett(LocalDate.of(2026, 12, 31), 3, 3),
                ArenaStatistikkForSisteDagMedRett(LocalDate.of(2027, 1, 31), 4, 3),
                ArenaStatistikkForSisteDagMedRett(null, 2, 2),
            )
            tac.settSisteDagMedRettFraArena(ArenaSisteDagMedRettEtter(ArenaSisteDagMedRettEtter.startdato, andre))
            tac.hentArenaStatistikkJobb.hentOgLagreSisteDagMedRett() shouldBe TaskResultat.IngenArbeid

            tac.clock.spolFrem(25.hours)
            tac.hentArenaStatistikkJobb.hentOgLagreSisteDagMedRett() shouldBe TaskResultat.Ferdig
            tac.lagretSisteDagMedRett() shouldBe andre
            tac.rettEtterDatoOgSluttmåned() shouldContainExactlyInAnyOrder listOf(
                Triple("etter 2026-11-30", 9, 8),
                Triple("etter 2026-12-15", 9, 8),
                Triple("sluttmåned 2026-12", 3, 3),
                Triple("sluttmåned 2027-01", 4, 3),
                Triple("sluttmåned åpen", 2, 2),
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter utbetalinger fra 2024 til og med neste år, venter et døgn og skriver over månedene`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(1.januar(2026))),
            runIsolated = true,
        ) { tac ->
            tac.leggTilUtbetalingerFraArena(2024, listOf(utbetalinger("2024-12", "5700.00")))
            tac.leggTilUtbetalingerFraArena(2025, listOf(utbetalinger("2025-01", "2850.00")))
            tac.leggTilUtbetalingerFraArena(2026, listOf(utbetalinger("2026-01", "2850.00")))
            tac.leggTilUtbetalingerFraArena(2027, listOf(utbetalinger("2027-01", "2850.00")))

            tac.hentArenaStatistikkJobb.hentOgLagreUtbetalinger() shouldBe TaskResultat.Ferdig
            tac.lagredeUtbetalinger() shouldBe listOf(
                utbetalinger("2024-12", "5700.00"),
                utbetalinger("2025-01", "2850.00"),
                utbetalinger("2026-01", "2850.00"),
                utbetalinger("2027-01", "2850.00"),
            )

            tac.leggTilUtbetalingerFraArena(2025, listOf(utbetalinger("2025-01", "2900.00"), utbetalinger("2025-02", "1425.00")))
            tac.hentArenaStatistikkJobb.hentOgLagreUtbetalinger() shouldBe TaskResultat.IngenArbeid

            tac.clock.spolFrem(25.hours)
            tac.hentArenaStatistikkJobb.hentOgLagreUtbetalinger() shouldBe TaskResultat.Ferdig
            tac.lagredeUtbetalinger() shouldBe listOf(
                utbetalinger("2024-12", "5700.00"),
                utbetalinger("2025-01", "2900.00"),
                utbetalinger("2025-02", "1425.00"),
                utbetalinger("2026-01", "2850.00"),
                utbetalinger("2027-01", "2850.00"),
            )
        }
    }

    @Test
    fun `utbetalingene lagres ikke når Arena svarer med feil`() = runTest {
        val repo = mockk<ArenaStatistikkRepo> { every { utbetalingerSistHentet() } returns null }
        val klient = mockk<TiltakspengerArenaClient> { coEvery { hentUtbetalingerPerMåned(any()) } returns uventetStatus().left() }

        HentArenaStatistikkJobb(klient, repo, TikkendeKlokke()).hentOgLagreUtbetalinger() shouldBe TaskResultat.Feilet
        verify(exactly = 0) { repo.lagreUtbetalinger(any(), any()) }
    }
}
