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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseKilde
import java.time.Clock
import java.time.LocalDateTime

class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    fun hent(id: TiltaksdeltakerHendelseId): TiltaksdeltakerHendelse? = sessionFactory.withSession {
        it.run(
            sqlQuery(
                """
                    select * 
                    from tiltaksdeltaker_kafka 
                    where hendelse_id = :hendelse_id
                """.trimIndent(),
                "hendelse_id" to id.toString(),
            ).map { row -> row.tilTiltaksdeltakerHendelse() }.asSingle,
        )
    }

    fun hentForDeltakerId(deltakerId: String): List<TiltaksdeltakerHendelse> = sessionFactory.withSession {
        it.run(
            sqlQuery(
                """
                    select * 
                    from tiltaksdeltaker_kafka 
                    where deltaker_id = :deltaker_id
                """.trimIndent(),
                "deltaker_id" to deltakerId,
            ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
        )
    }

    fun hentAlleUtenOppgaveEllerBehandling(sistOppdatertTidligereEnn: LocalDateTime): List<TiltaksdeltakerHendelse> =
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
                ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
            )
        }

    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        sistOppdatert: LocalDateTime = nå(clock),
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        insert into tiltaksdeltaker_kafka (
                            hendelse_id,
                            deltaker_id,
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
                            behandling_id,
                            kilde
                        ) values (
                            :hendelse_id,
                            :deltaker_id,
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
                            :behandling_id,
                            :kilde
                        )
                    """.trimIndent(),
                    "hendelse_id" to tiltaksdeltakerHendelse.id.toString(),
                    "deltaker_id" to tiltaksdeltakerHendelse.eksternDeltakerId,
                    "deltakelse_fra_og_med" to tiltaksdeltakerHendelse.deltakelseFraOgMed,
                    "deltakelse_til_og_med" to tiltaksdeltakerHendelse.deltakelseTilOgMed,
                    "dager_per_uke" to tiltaksdeltakerHendelse.dagerPerUke,
                    "deltakelsesprosent" to tiltaksdeltakerHendelse.deltakelsesprosent,
                    "deltakerstatus" to tiltaksdeltakerHendelse.deltakerstatus.name,
                    "sak_id" to tiltaksdeltakerHendelse.sakId.toString(),
                    "oppgave_id" to tiltaksdeltakerHendelse.oppgaveId?.toString(),
                    "sist_oppdatert" to sistOppdatert,
                    "melding" to melding,
                    "oppgave_sist_sjekket" to tiltaksdeltakerHendelse.oppgaveSistSjekket,
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "behandling_id" to tiltaksdeltakerHendelse.behandlingId?.toString(),
                    "kilde" to tiltaksdeltakerHendelse.kilde?.tilDb(),
                ).asUpdate,
            )
        }
    }

    fun slett(id: TiltaksdeltakerHendelseId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        delete from tiltaksdeltaker_kafka 
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun lagreOppgaveId(id: TiltaksdeltakerHendelseId, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set oppgave_id = :oppgave_id 
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "oppgave_id" to oppgaveId.toString(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun lagreBehandlingId(id: TiltaksdeltakerHendelseId, behandlingId: BehandlingId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandling_id = :behandling_id 
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandling_id" to behandlingId.toString(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    private fun Row.tilTiltaksdeltakerHendelse(): TiltaksdeltakerHendelse {
        return TiltaksdeltakerHendelse(
            id = TiltaksdeltakerHendelseId.fromString(string("hendelse_id")),
            eksternDeltakerId = string("deltaker_id"),
            deltakelseFraOgMed = localDateOrNull("deltakelse_fra_og_med"),
            deltakelseTilOgMed = localDateOrNull("deltakelse_til_og_med"),
            dagerPerUke = floatOrNull("dager_per_uke"),
            deltakelsesprosent = floatOrNull("deltakelsesprosent"),
            deltakerstatus = TiltakDeltakerstatus.valueOf(string("deltakerstatus")),
            sakId = SakId.fromString(string("sak_id")),
            oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
            oppgaveSistSjekket = localDateTimeOrNull("oppgave_sist_sjekket"),
            internDeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
            behandlingId = stringOrNull("behandling_id")?.let { BehandlingId.fromString(it) },
            kilde = stringOrNull("kilde")?.tilTiltaksdeltakerHendelseKilde(),
        )
    }
}

private enum class TiltaksdeltakerHendelseKildeDb {
    Arena,
    TeamTiltak,
    Komet,
}

private fun TiltaksdeltakerHendelseKilde.tilDb(): String = when (this) {
    TiltaksdeltakerHendelseKilde.Arena -> TiltaksdeltakerHendelseKildeDb.Arena
    TiltaksdeltakerHendelseKilde.TeamTiltak -> TiltaksdeltakerHendelseKildeDb.TeamTiltak
    TiltaksdeltakerHendelseKilde.Komet -> TiltaksdeltakerHendelseKildeDb.Komet
}.name

private fun String.tilTiltaksdeltakerHendelseKilde(): TiltaksdeltakerHendelseKilde =
    TiltaksdeltakerHendelseKildeDb.valueOf(this).let {
        when (it) {
            TiltaksdeltakerHendelseKildeDb.Arena -> TiltaksdeltakerHendelseKilde.Arena
            TiltaksdeltakerHendelseKildeDb.TeamTiltak -> TiltaksdeltakerHendelseKilde.TeamTiltak
            TiltaksdeltakerHendelseKildeDb.Komet -> TiltaksdeltakerHendelseKilde.Komet
        }
    }
