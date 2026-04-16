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
import org.jetbrains.annotations.TestOnly
import java.time.Clock
import java.time.LocalDateTime

class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {

    fun hentUbehandlede(minutterForsinkelse: Long = 0): List<TiltaksdeltakerHendelse> =
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        select * 
                        from tiltaksdeltaker_kafka
                        where behandlet_tidspunkt is null 
                          and sist_oppdatert < :sist_oppdatert
                        order by sist_oppdatert asc
                    """.trimIndent(),
                    "sist_oppdatert" to nå(clock).minusMinutes(minutterForsinkelse),
                ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
            )
        }

    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
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
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "behandling_id" to tiltaksdeltakerHendelse.behandlingId?.toString(),
                    "kilde" to kilde.name,
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletOgIgnorert(id: TiltaksdeltakerHendelseId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletMedOppgave(id: TiltaksdeltakerHendelseId, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt,
                            oppgave_id = :oppgave_id
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
                    "oppgave_id" to oppgaveId.toString(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletMedRevurdering(id: TiltaksdeltakerHendelseId, behandlingId: BehandlingId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt,
                            behandling_id = :behandling_id
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
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
            internDeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
            behandlingId = stringOrNull("behandling_id")?.let { BehandlingId.fromString(it) },
        )
    }

    @TestOnly
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

    @TestOnly
    fun hentForEksternDeltakerId(id: String): List<TiltaksdeltakerHendelse> = sessionFactory.withSession {
        it.run(
            sqlQuery(
                """
                    select * 
                    from tiltaksdeltaker_kafka 
                    where deltaker_id = :deltaker_id
                """.trimIndent(),
                "deltaker_id" to id,
            ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
        )
    }
}
