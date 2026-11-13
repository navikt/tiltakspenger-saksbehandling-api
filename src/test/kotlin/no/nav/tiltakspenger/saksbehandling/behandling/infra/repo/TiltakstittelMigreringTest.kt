package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
 * Legger titler inn i de lagrede tiltaksdeltakelsene slik de ble lagret før V279, og kjører V279 på nytt.
 * Etter migreringen skal titlene være borte, mens resten av json-en er urørt.
 */
class TiltakstittelMigreringTest {

    @Test
    @IsolatedDatabaseTest
    fun `fjerner tittel fra saksopplysningene og innvilgelsesperiodene`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val saksopplysninger = JSONObject(tac.hentJson(behandling.id, "saksopplysninger")).apply {
                put("tiltaksdeltakelserSomFratarRett", JSONArray().put(JSONObject().put("id", "TA9").put("tittel", "Lønnstilskudd hos Bedrift AS")))
                getJSONArray("tiltaksdeltagelse").forEachObject { it.put("tittel", "Sveisekurs hos Arrangør AS") }
            }
            val innvilgelsesperioder = JSONObject(tac.hentJson(behandling.id, "innvilgelsesperioder")).apply {
                getJSONArray("value").forEachObject { periode ->
                    periode.getJSONArray("tiltaksdeltakelser").forEachObject {
                        it.getJSONObject("tiltaksdeltakelse").put("tittel", "Sveisekurs hos Arrangør AS")
                    }
                }
                getJSONArray("value").put(JSONObject().put("tiltaksdeltakelser", JSONArray()))
            }
            tac.lagreJson(behandling.id, "saksopplysninger", saksopplysninger.toString())
            tac.lagreJson(behandling.id, "innvilgelsesperioder", innvilgelsesperioder.toString())
            tac.hentJson(behandling.id, "saksopplysninger") shouldContain "tittel"
            tac.hentJson(behandling.id, "innvilgelsesperioder") shouldContain "tittel"

            tac.kjørMigrering("V279__fjern_tittel_fra_tiltaksdeltakelser.sql")

            tac.hentJson(behandling.id, "saksopplysninger").let {
                it shouldNotContain "tittel"
                it shouldEqualJson saksopplysninger.utenTittel().toString()
            }
            tac.hentJson(behandling.id, "innvilgelsesperioder").let {
                it shouldNotContain "tittel"
                it shouldEqualJson innvilgelsesperioder.utenTittel().toString()
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `lar behandlinger uten tittel være urørt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val saksopplysninger = tac.hentJson(behandling.id, "saksopplysninger")
            val innvilgelsesperioder = tac.hentJson(behandling.id, "innvilgelsesperioder")

            tac.kjørMigrering("V279__fjern_tittel_fra_tiltaksdeltakelser.sql")

            tac.hentJson(behandling.id, "saksopplysninger") shouldEqualJson saksopplysninger
            tac.hentJson(behandling.id, "innvilgelsesperioder") shouldEqualJson innvilgelsesperioder
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id) shouldBe behandling
        }
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) block(getJSONObject(i))
    }

    /** Fjerner alle `tittel`-nøkler, uansett dybde. */
    private fun JSONObject.utenTittel(): JSONObject = JSONObject(toString()).also { it.fjernTittel() }

    private fun Any.fjernTittel() {
        when (this) {
            is JSONObject -> {
                remove("tittel")
                names()?.let { navn -> for (i in 0 until navn.length()) opt(navn.getString(i))?.fjernTittel() }
            }

            is JSONArray -> for (i in 0 until length()) opt(i)?.fjernTittel()
        }
    }

    private fun TestApplicationContextMedPostgres.hentJson(id: RammebehandlingId, kolonne: String): String =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "SELECT $kolonne FROM behandling WHERE id = :id",
                    "id" to id.toString(),
                ).map { row -> row.string(kolonne) }.asSingle,
            )
        }.shouldNotBeNull()

    private fun TestApplicationContextMedPostgres.lagreJson(id: RammebehandlingId, kolonne: String, json: String) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "UPDATE behandling SET $kolonne = :json::jsonb WHERE id = :id",
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
