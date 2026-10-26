package no.nav.tiltakspenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class StatistikkUtbetalingVedtakIdTest {

    /**
     * Meldeperioden stanses midt i, så meldekortbehandlingen bygger på både søknadsvedtaket og stansvedtaket.
     * Migreringen leser hele databasen, så den kjører isolert.
     */
    @Test
    @IsolatedDatabaseTest
    fun `utbetaling fra meldekortvedtak peker på rammevedtakene, og migreringen retter opp rader med meldekortvedtakets id`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            val førsteHverdagMedRett = meldeperiode.girRett
                .filter { (dato, harRett) -> harRett && dato.dayOfWeek < DayOfWeek.SATURDAY }
                .keys.min()
            val (_, stansvedtak) = iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = førsteHverdagMedRett.plusDays(1),
            )
            val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
            )!!

            val rammevedtakIder = listOf(søknadsvedtak.id, stansvedtak.id)
            meldekortvedtak.rammevedtak shouldBe rammevedtakIder
            tac.sessionFactory.hentVedtakIder(sak.id) shouldBe rammevedtakIder.map { it.toString() }

            // Slik prodkoden skrev raden før endringen.
            tac.sessionFactory.settVedtakId(sak.id, meldekortvedtak.id)
            tac.sessionFactory.kjørMigrering() shouldBe 1
            tac.sessionFactory.hentVedtakIder(sak.id) shouldBe rammevedtakIder.map { it.toString() }

            tac.sessionFactory.kjørMigrering() shouldBe 0
        }
    }
}

private fun PostgresSessionFactory.hentVedtakIder(sakId: SakId): List<String> = withSession { session ->
    session.run(
        sqlQuery(
            "select vedtak_id from statistikk_utbetaling where sak_id = :sak_id",
            "sak_id" to sakId.toString(),
        ).map { deserializeList<String>(it.string("vedtak_id")) }.asSingle,
    )
}!!

private fun PostgresSessionFactory.settVedtakId(sakId: SakId, vedtakId: VedtakId) = withSession { session ->
    session.run(
        sqlQuery(
            "update statistikk_utbetaling set vedtak_id = jsonb_build_array(:vedtak_id) where sak_id = :sak_id",
            "vedtak_id" to vedtakId.toString(),
            "sak_id" to sakId.toString(),
        ).asUpdate,
    )
}

private fun PostgresSessionFactory.kjørMigrering(): Int = withSession { session ->
    val sql = StatistikkUtbetalingVedtakIdTest::class.java
        .getResource("/db/migration/V265__statistikk_utbetaling_vedtak_id_rammevedtak.sql")!!
        .readText()
    session.run(sqlQuery(sql).asUpdate)
}
