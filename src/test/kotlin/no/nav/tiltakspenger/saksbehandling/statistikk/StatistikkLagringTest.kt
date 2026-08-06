package no.nav.tiltakspenger.saksbehandling.statistikk

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Statistikktabellene skrives av oss og leses kun av DVH, aldri av prodkoden vår.
 * Insert-SQLen har derfor ingen lesesti å bli verifisert gjennom, og en feil kolonnebinding ville aldri slått ut hos oss — den ville dukket opp som feil tall i datavarehuset.
 *
 * Denne testen kjører iverksettelsene gjennom prodstiene og leser radene tilbake med egen SQL.
 * Det er samme unntakskategori som de negative databasetestene: vi går utenom domenemodellen med vitende og vilje, jf. «Data som skrives, men aldri leses ut i domenet» i `../AGENTS-backend.md`.
 */
class StatistikkLagringTest {

    @Test
    fun `iverksettelse skriver saks-, stønads-, utbetalings- og meldekortstatistikk`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, rammevedtak) = iverksettSøknadsbehandling(tac = tac)
            val (_, meldekortvedtak, meldekortbehandling) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!

            val fnr = sak.fnr.verdi
            val saksnummer = sak.saksnummer.verdi
            val sessionFactory = tac.sessionFactory

            // statistikk_sak: én rad per hendelse i behandlingen, og DVH identifiserer hendelsen på behandlingid + endrettidspunkt.
            val saksstatistikk = sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.size shouldBeGreaterThan 0
            saksstatistikk.forEach {
                it.sakId shouldBe sak.id.toString()
                it.saksnummer shouldBe saksnummer
                it.fnr shouldBe fnr
                it.sakYtelse shouldBe "IND"
            }
            saksstatistikk.map { it.behandlingId }.distinct() shouldContainExactly listOf(
                rammevedtak.rammebehandling.id.toString(),
            )

            // statistikk_stonad: én rad per rammevedtak.
            sessionFactory.radFor(
                "select * from statistikk_stonad where sak_id = :sak_id",
                sak.id,
            ).let {
                it.string("bruker_id") shouldBe fnr
                it.string("saksnummer") shouldBe saksnummer
                it.string("vedtak_id") shouldBe rammevedtak.id.toString()
                it.string("soknad_id") shouldBe søknad.id.toString()
                it.string("vedtaksperiode") shouldBe rammevedtak.periode.tilDbPeriode()
                it.boolean("har_barnetillegg") shouldBe false
                it.string("fagsystem") shouldBe "TPSAK"
            }

            // statistikk_utbetaling: skrives av jobben som sender utbetalingen til helved.
            sessionFactory.radFor(
                "select * from statistikk_utbetaling where sak_id = :sak_id",
                sak.id,
            ).let {
                it.string("bruker_id") shouldBe fnr
                it.string("saksnummer") shouldBe saksnummer
                // DVH får uuid-delen av ULID-en, ikke den prefiksede id-en — jf. `V65__statistikk_utbetaling_add_utbetaling_id.sql`.
                it.string("utbetaling_id") shouldBe meldekortvedtak.utbetaling.id.uuidPart()
                it.int("belop") shouldBe it.int("ordinar_belop") + it.int("barnetillegg_belop")
                it.int("belop") shouldBeGreaterThan 0
            }

            // statistikk_meldekort: meldeperiodene og dagene ligger som jsonb, så vi sjekker at de faktisk ble serialisert inn.
            sessionFactory.radFor(
                "select * from statistikk_meldekort where sak_id = :sak_id",
                sak.id,
            ).let {
                it.string("bruker_id") shouldBe fnr
                it.string("saksnummer") shouldBe saksnummer
                it.string("meldekortbehandling_id") shouldBe meldekortbehandling.id.toString()
                it.boolean("behandlet_automatisk") shouldBe false
                it.string("periode") shouldBe meldekortbehandling.periode.tilDbPeriode()
                it.string("meldeperioder") shouldContain "meldeperiodeKjedeId"
                it.string("meldekortdager") shouldContain "dato"
            }
        }
    }
}

/** Leser den ene raden spørringen skal gi for saken, og feiler tydelig hvis den mangler. */
private fun PostgresSessionFactory.radFor(sql: String, sakId: SakId): Rad = withSession { session ->
    session.run(
        sqlQuery(sql, "sak_id" to sakId.toString()).map { row -> Rad(row) }.asSingle,
    )
} ?: throw AssertionError("Fant ingen rad for sakId $sakId med spørringen: $sql")

/**
 * Kotliquerys [Row] er bundet til den åpne sesjonen, så verdiene må leses ut før sesjonen lukkes.
 * Denne holder på de leste verdiene slik at assertene kan stå utenfor `withSession`.
 */
private class Rad(row: Row) {
    private val verdier: Map<String, Any?> = row.underlying.metaData.let { meta ->
        (1..meta.columnCount).associate { meta.getColumnLabel(it) to row.underlying.getObject(it) }
    }

    fun string(navn: String): String = verdier.getValue(navn).toString()

    fun int(navn: String): Int = (verdier.getValue(navn) as Number).toInt()

    fun boolean(navn: String): Boolean = verdier.getValue(navn) as Boolean

    fun localDate(navn: String): java.time.LocalDate = (verdier.getValue(navn) as java.sql.Date).toLocalDate()
}
