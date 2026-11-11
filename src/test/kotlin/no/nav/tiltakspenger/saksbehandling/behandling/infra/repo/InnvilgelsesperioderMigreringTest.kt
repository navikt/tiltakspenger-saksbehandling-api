package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Skriver innvilgelsesperiodene tilbake til formatet med én valgt tiltaksdeltakelse per periode, og kjører V278 på nytt.
 * Etter migreringen skal både den lagrede json-en og domenemodellen være lik det som ble lagret før.
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

    private fun TestApplicationContextMedPostgres.kjørMigrering(filnavn: String) {
        val migrering = javaClass.getResource("/db/migration/$filnavn").shouldNotBeNull().readText()
        sessionFactory.withSession { session -> session.run(sqlQuery(migrering).asExecute) }
    }
}
