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
 * Skriver innvilgelsesperiodene tilbake til et tidligere format, og kjører migreringene derfra på nytt.
 * Etter migreringene skal den lagrede json-en være lik det som ble lagret før, både for behandlingen og stønadsstatistikken.
 */
class InnvilgelsesperioderMigreringTest {

    @Test
    @IsolatedDatabaseTest
    fun `konverterer valgt tiltaksdeltakelse til en liste med én tiltaksdeltakelse`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val nyttFormat = tac.hentInnvilgelsesperioderJson(behandling.id)

            tac.lagreInnvilgelsesperioderJson(behandling.id, nyttFormat.tilFormatFørV278())
            tac.hentInnvilgelsesperioderJson(behandling.id).contains("valgtTiltaksdeltakelse") shouldBe true

            tac.kjørMigrering("V278__innvilgelsesperioder_flere_tiltaksdeltakelser.sql")
            tac.kjørMigrering("V279__innvilgelsesperioder_felles_antall_dager.sql")

            tac.hentInnvilgelsesperioderJson(behandling.id) shouldEqualJson nyttFormat
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).innvilgelsesperioder shouldBe behandling.innvilgelsesperioder
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `flytter antall dager fra tiltaksdeltakelsene til perioden`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val nyttFormat = tac.hentInnvilgelsesperioderJson(behandling.id)

            tac.lagreInnvilgelsesperioderJson(behandling.id, nyttFormat.tilFormatFraV278())
            tac.kjørMigrering("V279__innvilgelsesperioder_felles_antall_dager.sql")

            tac.hentInnvilgelsesperioderJson(behandling.id) shouldEqualJson nyttFormat
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).innvilgelsesperioder shouldBe behandling.innvilgelsesperioder

            // Kjøres migreringen på nytt, beholdes perioden som den er.
            tac.kjørMigrering("V279__innvilgelsesperioder_felles_antall_dager.sql")
            tac.hentInnvilgelsesperioderJson(behandling.id) shouldEqualJson nyttFormat
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `summerer antall dager for flere tiltaksdeltakelser og beholder rekkefølgen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val behandling = iverksettSøknadsbehandling(tac = tac).third.rammebehandling
            val opprettet = JSONObject(tac.hentInnvilgelsesperioderJson(behandling.id)).getJSONArray("value").getJSONObject(0)
            val første = opprettet.getJSONArray("tiltaksdeltakelser").getJSONObject(0)
            val andre = JSONObject(første.toString()).put("eksternDeltagelseId", "en-annen-deltakelse")
            val periode = opprettet.getJSONObject("periode")
            val reversertOpphør = JSONObject().put("opprinneligVedtakId", "vedtak_01JQ0000000000000000000000")

            tac.lagreInnvilgelsesperioderJson(
                behandling.id,
                """
                {
                  "value": [
                    {
                      "periode": $periode,
                      "tiltaksdeltakelser": [
                        {"tiltaksdeltakelse": $andre, "antallDagerPerMeldeperiode": 4},
                        {"tiltaksdeltakelse": $første, "antallDagerPerMeldeperiode": 6}
                      ]
                    },
                    {
                      "periode": {"fraOgMed": "2030-01-01", "tilOgMed": "2030-01-31"},
                      "tiltaksdeltakelser": [{"tiltaksdeltakelse": $første, "antallDagerPerMeldeperiode": 8}],
                      "reversertOpphør": $reversertOpphør
                    }
                  ]
                }
                """.trimIndent(),
            )
            tac.kjørMigrering("V279__innvilgelsesperioder_felles_antall_dager.sql")

            tac.hentInnvilgelsesperioderJson(behandling.id) shouldEqualJson """
                {
                  "value": [
                    {
                      "periode": $periode,
                      "tiltaksdeltakelser": [$andre, $første],
                      "antallDagerPerMeldeperiode": 10
                    },
                    {
                      "periode": {"fraOgMed": "2030-01-01", "tilOgMed": "2030-01-31"},
                      "tiltaksdeltakelser": [$første],
                      "antallDagerPerMeldeperiode": 8,
                      "reversertOpphør": $reversertOpphør
                    }
                  ]
                }
            """.trimIndent()
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
    private fun String.tilFormatFørV278(): String {
        val perioder = JSONObject(this).getJSONArray("value")
        val gamlePerioder = JSONArray()
        for (i in 0 until perioder.length()) {
            val periode = perioder.getJSONObject(i)
            val tiltaksdeltakelse = periode.getJSONArray("tiltaksdeltakelser").also { it.length() shouldBe 1 }.getJSONObject(0)
            periode.remove("tiltaksdeltakelser")
            periode.put("valgtTiltaksdeltakelse", tiltaksdeltakelse)
            gamlePerioder.put(periode)
        }
        return JSONObject().put("value", gamlePerioder).toString()
    }

    /** Gjenskaper formatet fra V278, der hver tiltaksdeltakelse i perioden hadde sitt eget `antallDagerPerMeldeperiode`. */
    private fun String.tilFormatFraV278(): String {
        val perioder = JSONObject(this).getJSONArray("value")
        val gamlePerioder = JSONArray()
        for (i in 0 until perioder.length()) {
            val periode = perioder.getJSONObject(i)
            val antallDager = periode.remove("antallDagerPerMeldeperiode").shouldNotBeNull()
            val tiltaksdeltakelse = periode.getJSONArray("tiltaksdeltakelser").also { it.length() shouldBe 1 }.getJSONObject(0)
            periode.put(
                "tiltaksdeltakelser",
                JSONArray().put(JSONObject().put("tiltaksdeltakelse", tiltaksdeltakelse).put("antallDagerPerMeldeperiode", antallDager)),
            )
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
