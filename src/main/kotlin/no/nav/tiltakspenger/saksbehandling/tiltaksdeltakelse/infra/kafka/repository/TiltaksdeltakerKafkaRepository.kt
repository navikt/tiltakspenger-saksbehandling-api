package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock
import java.time.LocalDateTime

class TiltaksdeltakerKafkaRepository(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    fun hent(id: String): TiltaksdeltakerKafkaDb? = sessionFactory.withSession {
        it.run(
            sqlQuery(
                """
                    select * 
                    from tiltaksdeltaker_kafka 
                    where id = :id
                """.trimIndent(),
                "id" to id,
            ).map { row -> row.toTiltaksdeltakerKafkaDb() }.asSingle,
        )
    }

    fun hentAlleUtenOppgaveEllerBehandling(sistOppdatertTidligereEnn: LocalDateTime): List<TiltaksdeltakerKafkaDb> =
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        select * 
                        from tiltaksdeltaker_kafka
                        where oppgave_id is null 
                          and behandling_id is null 
                          and sist_oppdatert < :sist_oppdatert
                    """.trimIndent(),
                    "sist_oppdatert" to sistOppdatertTidligereEnn,
                ).map { row -> row.toTiltaksdeltakerKafkaDb() }.asList,
            )
        }

    fun lagre(
        tiltaksdeltakerKafkaDb: TiltaksdeltakerKafkaDb,
        melding: String,
        sistOppdatert: LocalDateTime = nå(clock),
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
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
                            oppgave_sist_sjekket,
                            tiltaksdeltaker_id,
                            behandling_id
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
                            :oppgave_sist_sjekket,
                            :tiltaksdeltaker_id,
                            :behandling_id
                        ) on conflict (id) do update set
                            deltakelse_fra_og_med = :deltakelse_fra_og_med,
                            deltakelse_til_og_med = :deltakelse_til_og_med,
                            dager_per_uke = :dager_per_uke,
                            deltakelsesprosent = :deltakelsesprosent,
                            deltakerstatus = :deltakerstatus,
                            sist_oppdatert = :sist_oppdatert,
                            melding = :melding
                    """.trimIndent(),
                    "id" to tiltaksdeltakerKafkaDb.id,
                    "deltakelse_fra_og_med" to tiltaksdeltakerKafkaDb.deltakelseFraOgMed,
                    "deltakelse_til_og_med" to tiltaksdeltakerKafkaDb.deltakelseTilOgMed,
                    "dager_per_uke" to tiltaksdeltakerKafkaDb.dagerPerUke,
                    "deltakelsesprosent" to tiltaksdeltakerKafkaDb.deltakelsesprosent,
                    "deltakerstatus" to tiltaksdeltakerKafkaDb.deltakerstatus.name,
                    "sak_id" to tiltaksdeltakerKafkaDb.sakId.toString(),
                    "oppgave_id" to tiltaksdeltakerKafkaDb.oppgaveId?.toString(),
                    "sist_oppdatert" to sistOppdatert,
                    "melding" to melding,
                    "oppgave_sist_sjekket" to tiltaksdeltakerKafkaDb.oppgaveSistSjekket,
                    "tiltaksdeltaker_id" to tiltaksdeltakerKafkaDb.tiltaksdeltakerId.toString(),
                    "behandling_id" to tiltaksdeltakerKafkaDb.behandlingId?.toString(),
                ).asUpdate,
            )
        }
    }

    fun slett(id: String) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        delete from tiltaksdeltaker_kafka 
                        where id = :id
                    """.trimIndent(),
                    "id" to id,
                ).asUpdate,
            )
        }
    }

    fun lagreOppgaveId(id: String, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set oppgave_id = :oppgave_id 
                        where id = :id
                    """.trimIndent(),
                    "oppgave_id" to oppgaveId.toString(),
                    "id" to id,
                ).asUpdate,
            )
        }
    }

    fun lagreBehandlingId(id: String, behandlingId: BehandlingId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandling_id = :behandling_id 
                        where id = :id
                    """.trimIndent(),
                    "behandling_id" to behandlingId.toString(),
                    "id" to id,
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
            tiltaksdeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
            behandlingId = stringOrNull("behandling_id")?.let { BehandlingId.fromString(it) },
        )
}
