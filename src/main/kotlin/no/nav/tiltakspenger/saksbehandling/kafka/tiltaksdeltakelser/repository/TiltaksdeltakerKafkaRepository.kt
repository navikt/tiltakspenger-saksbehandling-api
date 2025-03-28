package no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.repository

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class TiltaksdeltakerKafkaRepository(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun hent(id: String): TiltaksdeltakerKafkaDb? = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentForId, id)
                .map { row -> row.toTiltaksdeltakerKafkaDb() }
                .asSingle,
        )
    }

    fun hentAlleUtenOppgave(): List<TiltaksdeltakerKafkaDb> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentAlleUtenOppgave)
                .map { row -> row.toTiltaksdeltakerKafkaDb() }
                .asList,
        )
    }

    fun hentAlleMedOppgave(
        oppgaveSistSjekket: LocalDateTime = LocalDateTime.now().minusHours(1),
    ): List<TiltaksdeltakerKafkaDb> = sessionFactory.withSession {
        it.run(
            queryOf(
                """
                    select *
                    from tiltaksdeltaker_kafka
                    where oppgave_id is not null
                      and (oppgave_sist_sjekket is null or oppgave_sist_sjekket < :oppgave_sist_sjekket)
                """.trimIndent(),
                mapOf(
                    "oppgave_sist_sjekket" to oppgaveSistSjekket,
                ),
            ).map { row -> row.toTiltaksdeltakerKafkaDb() }
                .asList,
        )
    }

    fun lagre(tiltaksdeltakerKafkaDb: TiltaksdeltakerKafkaDb, melding: String) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    lagreTiltaksdeltakerKafka,
                    mapOf(
                        "id" to tiltaksdeltakerKafkaDb.id,
                        "deltakelse_fra_og_med" to tiltaksdeltakerKafkaDb.deltakelseFraOgMed,
                        "deltakelse_til_og_med" to tiltaksdeltakerKafkaDb.deltakelseTilOgMed,
                        "dager_per_uke" to tiltaksdeltakerKafkaDb.dagerPerUke,
                        "deltakelsesprosent" to tiltaksdeltakerKafkaDb.deltakelsesprosent,
                        "deltakerstatus" to tiltaksdeltakerKafkaDb.deltakerstatus.name,
                        "sak_id" to tiltaksdeltakerKafkaDb.sakId.toString(),
                        "oppgave_id" to tiltaksdeltakerKafkaDb.oppgaveId?.toString(),
                        "sist_oppdatert" to LocalDateTime.now(),
                        "melding" to melding,
                        "oppgave_sist_sjekket" to tiltaksdeltakerKafkaDb.oppgaveSistSjekket,
                    ),
                ).asUpdate,
            )
        }
    }

    fun slett(id: String) {
        sessionFactory.withSession {
            it.run(
                queryOf(sqlSlettForId, id).asUpdate,
            )
        }
    }

    fun lagreOppgaveId(id: String, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update tiltaksdeltaker_kafka set oppgave_id = :oppgave_id where id = :id
                    """.trimIndent(),
                    mapOf(
                        "oppgave_id" to oppgaveId.toString(),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

    fun oppdaterOppgaveSistSjekket(id: String) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update tiltaksdeltaker_kafka set oppgave_sist_sjekket = :oppgave_sist_sjekket where id = :id
                    """.trimIndent(),
                    mapOf(
                        "oppgave_sist_sjekket" to LocalDateTime.now(),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun Row.toTiltaksdeltakerKafkaDb() =
        TiltaksdeltakerKafkaDb(
            id = string("id"),
            deltakelseFraOgMed = localDateOrNull("deltakelse_fra_og_med"),
            deltakelseTilOgMed = localDateOrNull("deltakelse_til_og_med"),
            dagerPerUke = floatOrNull("dager_per_uke"),
            deltakelsesprosent = floatOrNull("deltakelsesprosent"),
            deltakerstatus = TiltakDeltakerstatus.valueOf(string("deltakerstatus")),
            sakId = SakId.fromString(string("sak_id")),
            oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
            oppgaveSistSjekket = localDateTimeOrNull("oppgave_sist_sjekket"),
        )

    @Language("SQL")
    private val lagreTiltaksdeltakerKafka =
        """
        insert into tiltaksdeltaker_kafka (
            id,
            deltakelse_fra_og_med,
            deltakelse_til_og_med,
            dager_per_uke,
            deltakelsesprosent,
            deltakerstatus,
            sak_id,
            oppgave_id,
            sist_oppdatert,
            melding,
            oppgave_sist_sjekket
        ) values (
            :id,
            :deltakelse_fra_og_med,
            :deltakelse_til_og_med,
            :dager_per_uke,
            :deltakelsesprosent,
            :deltakerstatus,
            :sak_id,
            :oppgave_id,
            :sist_oppdatert,
            :melding,
            :oppgave_sist_sjekket
        ) ON CONFLICT (id) DO UPDATE SET
            deltakelse_fra_og_med = :deltakelse_fra_og_med,
            deltakelse_til_og_med = :deltakelse_til_og_med,
            dager_per_uke = :dager_per_uke,
            deltakelsesprosent = :deltakelsesprosent,
            deltakerstatus = :deltakerstatus,
            sist_oppdatert = :sist_oppdatert,
            melding = :melding
        """.trimIndent()

    @Language("SQL")
    private val sqlHentForId = "select * from tiltaksdeltaker_kafka where id = ?"

    @Language("SQL")
    private val sqlHentAlleUtenOppgave = "select * from tiltaksdeltaker_kafka where oppgave_id is null"

    @Language("SQL")
    private val sqlSlettForId = "delete from tiltaksdeltaker_kafka where id = ?"
}
