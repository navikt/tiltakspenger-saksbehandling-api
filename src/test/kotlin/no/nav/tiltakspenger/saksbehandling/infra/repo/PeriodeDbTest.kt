package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotliquery.Session
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.periodeOrNull
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Tester for egendefinert postgres type periode_datoer. Se V200__periode_type.sql
class PeriodeDbTest {

    private fun withPeriodeTestTabell(test: (Session) -> Unit) {
        withMigratedDb { testDataHelper ->
            testDataHelper.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        CREATE TEMP TABLE IF NOT EXISTS test_periode (
                            id SERIAL PRIMARY KEY,
                            name TEXT,
                            p periode
                        );
                        TRUNCATE test_periode RESTART IDENTITY
                        """.trimIndent(),
                    ).asUpdate,
                )
                test(session)
            }
        }
    }

    @Test
    fun `skal persistere og gjenopprette en periode`() {
        withPeriodeTestTabell { session ->
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (p) VALUES (:periode::periode)
                    """.trimIndent(),
                    "periode" to periode.tilDbPeriode(),
                ).asUpdate,
            )

            val result = session.run(
                sqlQuery(
                    """
                    SELECT p FROM test_periode WHERE id = 1
                    """.trimIndent(),
                ).map { row -> row.periode("p") }.asSingle,
            )

            result shouldBe periode
        }
    }

    @Test
    fun `skal kunne selektere periode som daterange`() {
        withPeriodeTestTabell { session ->
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (p) VALUES (:periode::periode)
                    """.trimIndent(),
                    "periode" to periode.tilDbPeriode(),
                ).asUpdate,
            )

            // Cast periode to daterange and verify bounds
            val result = session.run(
                sqlQuery(
                    """
                    SELECT 
                        lower(p::daterange) as lower_bound,
                        upper(p::daterange) as upper_bound,
                        lower_inc(p::daterange) as lower_inclusive,
                        upper_inc(p::daterange) as upper_inclusive
                    FROM test_periode WHERE id = 1
                    """.trimIndent(),
                ).map { row ->
                    mapOf(
                        "lower" to row.localDate("lower_bound"),
                        "upper" to row.localDate("upper_bound"),
                        "lowerInc" to row.boolean("lower_inclusive"),
                        "upperInc" to row.boolean("upper_inclusive"),
                    )
                }.asSingle,
            )

            result!!["lower"] shouldBe periode.fraOgMed
            // upper er eksklusiv i daterange-representasjon, så det er dagen etter til_og_med
            result["upper"] shouldBe periode.tilOgMed.plusDays(1)
            result["lowerInc"] shouldBe true
            // upper_inc er false fordi daterange lagrer [lower, upper) internt
            result["upperInc"] shouldBe false
        }
    }

    @Test
    fun `skal kunne sammenligne periode med dato ved bruk av daterange`() {
        withPeriodeTestTabell { session ->
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (p) VALUES (:periode::periode)
                    """.trimIndent(),
                    "periode" to periode.tilDbPeriode(),
                ).asUpdate,
            )

            // Dato innenfor perioden
            val containsMiddle = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> '2026-01-15'::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            containsMiddle shouldBe true

            // Første dag i perioden (inklusiv)
            val containsFirst = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> :dato::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                    "dato" to periode.fraOgMed,
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            containsFirst shouldBe true

            // Siste dag i perioden (inklusiv)
            val containsLast = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> :dato::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                    "dato" to periode.tilOgMed,
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            containsLast shouldBe true

            // Dagen før perioden
            val containsBefore = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> :dato::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                    "dato" to periode.fraOgMed.minusDays(1),
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            containsBefore shouldBe false

            // Dagen etter perioden
            val containsAfter = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> :dato::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                    "dato" to periode.tilOgMed.plusDays(1),
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            containsAfter shouldBe false
        }
    }

    @Test
    fun `skal kunne sammenligne to perioder ved bruk av daterange overlaps`() {
        withPeriodeTestTabell { session ->
            val januar = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
            val februar = Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
            val janFeb = Periode(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (name, p) VALUES 
                        ('januar', :januar::periode),
                        ('februar', :februar::periode),
                        ('jan-feb', :janFeb::periode)
                    """.trimIndent(),
                    "januar" to januar.tilDbPeriode(),
                    "februar" to februar.tilDbPeriode(),
                    "janFeb" to janFeb.tilDbPeriode(),
                ).asUpdate,
            )

            // januar overlapper med jan-feb
            val januarOverlapsJanFeb = session.run(
                sqlQuery(
                    """
                    SELECT (SELECT p FROM test_periode WHERE name = 'januar')::daterange && 
                           (SELECT p FROM test_periode WHERE name = 'jan-feb')::daterange as overlaps
                    """.trimIndent(),
                ).map { row -> row.boolean("overlaps") }.asSingle,
            )
            januarOverlapsJanFeb shouldBe true

            // februar overlapper med jan-feb
            val februarOverlapsJanFeb = session.run(
                sqlQuery(
                    """
                    SELECT (SELECT p FROM test_periode WHERE name = 'februar')::daterange && 
                           (SELECT p FROM test_periode WHERE name = 'jan-feb')::daterange as overlaps
                    """.trimIndent(),
                ).map { row -> row.boolean("overlaps") }.asSingle,
            )
            februarOverlapsJanFeb shouldBe true

            // januar overlapper IKKE med februar
            val januarOverlapsFebruar = session.run(
                sqlQuery(
                    """
                    SELECT (SELECT p FROM test_periode WHERE name = 'januar')::daterange && 
                           (SELECT p FROM test_periode WHERE name = 'februar')::daterange as overlaps
                    """.trimIndent(),
                ).map { row -> row.boolean("overlaps") }.asSingle,
            )
            januarOverlapsFebruar shouldBe false
        }
    }

    @Test
    fun `periode domain skal avvise ugyldig periode hvor fra_og_med er etter til_og_med`() {
        withPeriodeTestTabell { session ->
            val exception = shouldThrow<org.postgresql.util.PSQLException> {
                session.run(
                    sqlQuery(
                        """
                        INSERT INTO test_periode (p) VALUES (ROW('2026-01-31', '2026-01-01')::periode)
                        """.trimIndent(),
                    ).asUpdate,
                )
            }

            exception.message shouldContain "periode_check"
        }
    }

    @Test
    fun `periode domain skal tillate null verdier`() {
        withPeriodeTestTabell { session ->
            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (p) VALUES (NULL)
                    """.trimIndent(),
                ).asUpdate,
            )

            val result = session.run(
                sqlQuery(
                    """
                    SELECT p FROM test_periode WHERE id = 1
                    """.trimIndent(),
                ).map { row -> row.periodeOrNull("p") }.asSingle,
            )

            result shouldBe null
        }
    }

    @Test
    fun `periode med samme dag som fra_og_med og til_og_med skal være gyldig`() {
        withPeriodeTestTabell { session ->
            val periode = Periode(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (p) VALUES (:periode::periode)
                    """.trimIndent(),
                    "periode" to periode.tilDbPeriode(),
                ).asUpdate,
            )

            val result = session.run(
                sqlQuery(
                    """
                    SELECT p FROM test_periode WHERE id = 1
                    """.trimIndent(),
                ).map { row -> row.periode("p") }.asSingle,
            )

            result shouldBe periode

            // Verifiser at en-dags periode inneholder datoen
            val contains = session.run(
                sqlQuery(
                    """
                    SELECT p::daterange @> :dato::date as contains FROM test_periode WHERE id = 1
                    """.trimIndent(),
                    "dato" to periode.fraOgMed,
                ).map { row -> row.boolean("contains") }.asSingle,
            )
            contains shouldBe true
        }
    }

    @Test
    fun `skal kunne bruke periode i where-clause med daterange operatorer`() {
        withPeriodeTestTabell { session ->
            val q1 = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
            val q2 = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30))
            val q3 = Periode(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30))
            val q4 = Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31))

            session.run(
                sqlQuery(
                    """
                    INSERT INTO test_periode (name, p) VALUES 
                        ('q1', :q1::periode),
                        ('q2', :q2::periode),
                        ('q3', :q3::periode),
                        ('q4', :q4::periode)
                    """.trimIndent(),
                    "q1" to q1.tilDbPeriode(),
                    "q2" to q2.tilDbPeriode(),
                    "q3" to q3.tilDbPeriode(),
                    "q4" to q4.tilDbPeriode(),
                ).asUpdate,
            )

            // Finn alle perioder som inneholder en spesifikk dato
            val periodsContainingMay = session.run(
                sqlQuery(
                    """
                    SELECT name FROM test_periode WHERE p::daterange @> '2026-05-15'::date
                    """.trimIndent(),
                ).map { row -> row.string("name") }.asList,
            )
            periodsContainingMay shouldBe listOf("q2")

            // Finn alle perioder som overlapper med et gitt intervall
            val searchPeriode = Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30))
            val periodsOverlappingFebToApr = session.run(
                sqlQuery(
                    """
                    SELECT name FROM test_periode 
                    WHERE p::daterange && :searchPeriode::daterange
                    ORDER BY name
                    """.trimIndent(),
                    "searchPeriode" to searchPeriode.tilDbPeriode(),
                ).map { row -> row.string("name") }.asList,
            )
            periodsOverlappingFebToApr shouldBe listOf("q1", "q2")
        }
    }
}
