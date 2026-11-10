package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test

class UtbetalingskvitteringMigreringTest {
    @Test
    fun `V277 oppretter innboksen med koblinger og retry fra starten`() {
        withTestApplicationContextAndPostgres { tac ->
            tac.sessionFactory.withSession { session ->
                val nullableKolonner = session.run(
                    sqlQuery(
                        """
                        SELECT attname
                        FROM pg_attribute
                        WHERE attrelid = 'utbetalingskvittering'::regclass
                          AND attnum > 0
                          AND NOT attisdropped
                          AND NOT attnotnull
                        """,
                    ).map { it.string("attname") }.asList,
                )
                nullableKolonner.toSet() shouldBe setOf(
                    "key",
                    "value",
                    "behandlet",
                    "behandlet_feil",
                    "sak_id",
                    "utbetaling_id",
                    "forsokshistorikk",
                    "forbigaaende",
                )

                val fremmednøkler = session.run(
                    sqlQuery(
                        """
                        SELECT a.attname, c.confrelid::regclass::text AS tabell
                        FROM pg_constraint c
                        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
                        WHERE c.conrelid = 'utbetalingskvittering'::regclass
                          AND c.contype = 'f'
                        """,
                    ).map { it.string("attname") to it.string("tabell") }.asList,
                )
                fremmednøkler.toSet() shouldBe setOf("sak_id" to "sak", "utbetaling_id" to "utbetaling")

                val indekser = session.run(
                    sqlQuery(
                        """
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = current_schema()
                          AND (
                            tablename = 'utbetalingskvittering'
                            OR (tablename = 'utbetaling' AND indexname = 'utbetaling_uuid_part_idx')
                          )
                        """,
                    ).map { it.string("indexname") }.asList,
                )
                indekser.toSet() shouldBe setOf(
                    "utbetalingskvittering_pkey",
                    "utbetalingskvittering_kafka_unik",
                    "utbetalingskvittering_ubehandlet_idx",
                    "utbetalingskvittering_ubehandlet_topic_partisjon_key_offset_idx",
                    "utbetaling_uuid_part_idx",
                )
            }
        }
    }
}
