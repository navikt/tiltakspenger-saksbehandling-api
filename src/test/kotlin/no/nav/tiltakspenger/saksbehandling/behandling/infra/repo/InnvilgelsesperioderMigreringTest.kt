package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Skriver innvilgelsesperiodene tilbake til formatet fra før V278, og kjører V278 på nytt.
 * Etter migreringen skal den lagrede json-en være lik det som ble lagret før, både for behandlingen og stønadsstatistikken.
 */
class InnvilgelsesperioderMigreringTest {

    @Test
    @IsolatedDatabaseTest
    fun `konverterer valgt tiltaksdeltakelse til en liste med én tiltaksdeltakelse`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val nyttFormat = tac.hentInnvilgelsesperioderJson(behandling.id)

            tac.lagreInnvilgelsesperioderJson(behandling.id, nyttFormat.tilGammeltFormat())
            tac.hentInnvilgelsesperioderJson(behandling.id).contains("valgtTiltaksdeltakelse") shouldBe true

            tac.kjørMigrering("V278__innvilgelsesperioder_flere_tiltaksdeltakelser.sql")

            tac.hentInnvilgelsesperioderJson(behandling.id) shouldEqualJson nyttFormat
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).innvilgelsesperioder shouldBe behandling.innvilgelsesperioder
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kopierer tiltaksdeltakelse til en liste i stønadsstatistikken`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val vedtak = iverksettSøknadsbehandling(tac = tac).third
            val nyttFormat = tac.hentStatistikkInnvilgelsesperioderJson(vedtak.id)
            val gammeltFormat = JSONArray(nyttFormat).apply {
                for (i in 0 until length()) getJSONObject(i).remove("tiltaksdeltakelser").shouldNotBeNull()
            }.toString()
            val utenTiltaksdeltakelse = JSONArray(gammeltFormat).apply {
                for (i in 0 until length()) getJSONObject(i).remove("tiltaksdeltakelse")
            }.toString()
            val forventetUtenTiltaksdeltakelse = JSONArray(utenTiltaksdeltakelse).apply {
                for (i in 0 until length()) getJSONObject(i).put("tiltaksdeltakelser", JSONArray())
            }.toString()

            tac.lagreStatistikkInnvilgelsesperioderJson(vedtak.id, gammeltFormat)
            tac.kjørMigrering("V278__innvilgelsesperioder_flere_tiltaksdeltakelser.sql")
            tac.hentStatistikkInnvilgelsesperioderJson(vedtak.id) shouldEqualJson nyttFormat

            // Kjøres migreringen på nytt, beholdes listen som den er.
            tac.kjørMigrering("V278__innvilgelsesperioder_flere_tiltaksdeltakelser.sql")
            tac.hentStatistikkInnvilgelsesperioderJson(vedtak.id) shouldEqualJson nyttFormat

            tac.lagreStatistikkInnvilgelsesperioderJson(vedtak.id, utenTiltaksdeltakelse)
            tac.kjørMigrering("V278__innvilgelsesperioder_flere_tiltaksdeltakelser.sql")
            tac.hentStatistikkInnvilgelsesperioderJson(vedtak.id) shouldEqualJson forventetUtenTiltaksdeltakelse
        }
    }

    /** Gjenskaper formatet fra før V278, der hver periode hadde `valgtTiltaksdeltakelse` og `antallDagerPerMeldeperiode`. */
    private fun String.tilGammeltFormat(): String {
        val perioder = JSONObject(this).getJSONArray("value")
        val gamlePerioder = JSONArray()
        for (i in 0 until perioder.length()) {
            val periode = perioder.getJSONObject(i)
            val tiltaksdeltakelse = periode.getJSONArray("tiltaksdeltakelser").also { it.length() shouldBe 1 }.getJSONObject(0)
            periode.remove("tiltaksdeltakelser")
            periode.put("valgtTiltaksdeltakelse", tiltaksdeltakelse.getJSONObject("tiltaksdeltakelse"))
            periode.put("antallDagerPerMeldeperiode", tiltaksdeltakelse.getInt("antallDagerPerMeldeperiode"))
            gamlePerioder.put(periode)
        }
        return JSONObject().put("value", gamlePerioder).toString()
    }

    private fun TestApplicationContextMedPostgres.hentInnvilgelsesperioderJson(id: RammebehandlingId): String =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "SELECT innvilgelsesperioder FROM behandling WHERE id = :id",
                    "id" to id.toString(),
                ).map { row -> row.string("innvilgelsesperioder") }.asSingle,
            )
        }.shouldNotBeNull()

    private fun TestApplicationContextMedPostgres.lagreInnvilgelsesperioderJson(id: RammebehandlingId, json: String) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "UPDATE behandling SET innvilgelsesperioder = :json::jsonb WHERE id = :id",
                    "json" to json,
                    "id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContextMedPostgres.hentStatistikkInnvilgelsesperioderJson(vedtakId: VedtakId): String =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "SELECT innvilgelsesperioder FROM statistikk_stonad WHERE vedtak_id = :vedtakId",
                    "vedtakId" to vedtakId.toString(),
                ).map { row -> row.string("innvilgelsesperioder") }.asSingle,
            )
        }.shouldNotBeNull()

    private fun TestApplicationContextMedPostgres.lagreStatistikkInnvilgelsesperioderJson(vedtakId: VedtakId, json: String) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "UPDATE statistikk_stonad SET innvilgelsesperioder = :json::jsonb WHERE vedtak_id = :vedtakId",
                    "json" to json,
                    "vedtakId" to vedtakId.toString(),
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContextMedPostgres.kjørMigrering(filnavn: String) {
        val migrering = javaClass.getResource("/db/migration/$filnavn").shouldNotBeNull().readText()
        sessionFactory.withSession { session -> session.run(sqlQuery(migrering).asExecute) }
    }
}
