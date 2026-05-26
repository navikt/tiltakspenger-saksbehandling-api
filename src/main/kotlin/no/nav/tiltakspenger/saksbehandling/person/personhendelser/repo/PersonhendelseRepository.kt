package no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import tools.jackson.module.kotlin.readValue
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class PersonhendelseRepository(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    fun hent(sakId: SakId): List<PersonhendelseDb> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """select * from personhendelse where sak_id = :sak_id""",
                    mapOf("sak_id" to sakId.toString()),
                ).map { row -> row.toPersonhendelseDb() }.asList,
            )
        }
    }

    fun hentAlleUtenOppgave(): List<PersonhendelseDb> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """select * from personhendelse where oppgave_id is null""",
                ).map { row -> row.toPersonhendelseDb() }.asList,
            )
        }
    }

    /**
     * Henter kun de hvor oppgave_sist_sjekket er null eller oppgave_sist_sjekket < [oppgaveSistSjekket]
     */
    fun hentAlleMedOppgave(
        oppgaveSistSjekket: LocalDateTime = nå(clock).minusHours(1),
    ): List<PersonhendelseDb> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        select *
                        from personhendelse
                        where oppgave_id is not null
                          and (oppgave_sist_sjekket is null or oppgave_sist_sjekket < :oppgave_sist_sjekket)
                    """.trimIndent(),
                    mapOf(
                        "oppgave_sist_sjekket" to oppgaveSistSjekket,
                    ),
                ).map { row -> row.toPersonhendelseDb() }.asList,
            )
        }
    }

    fun lagre(personhendelseDb: PersonhendelseDb) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                        INSERT INTO personhendelse (
                            id,
                            fnr,
                            hendelse_id,
                            opplysningstype,
                            personhendelse_type,
                            sak_id,
                            oppgave_id,
                            sist_oppdatert,
                            oppgave_sist_sjekket
                        ) VALUES (
                            :id,
                            :fnr,
                            :hendelse_id,
                            :opplysningstype,
                            :personhendelse_type,
                            :sak_id,
                            :oppgave_id,
                            :sist_oppdatert,
                            :oppgave_sist_sjekket
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to personhendelseDb.id,
                        "fnr" to personhendelseDb.fnr.verdi,
                        "hendelse_id" to personhendelseDb.hendelseId,
                        "opplysningstype" to personhendelseDb.opplysningstype.name,
                        "personhendelse_type" to toPGObject(personhendelseDb.personhendelseType),
                        "sak_id" to personhendelseDb.sakId.toString(),
                        "oppgave_id" to personhendelseDb.oppgaveId?.toString(),
                        "sist_oppdatert" to nå(clock),
                        "oppgave_sist_sjekket" to personhendelseDb.oppgaveSistSjekket,
                    ),
                ).asUpdate,
            )
        }
    }

    fun slett(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """delete from personhendelse where id = :id""",
                    mapOf("id" to id),
                ).asUpdate,
            )
        }
    }

    fun lagreOppgaveId(id: UUID, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """update personhendelse set oppgave_id = :oppgave_id where id = :id""",
                    mapOf("oppgave_id" to oppgaveId.toString(), "id" to id),
                ).asUpdate,
            )
        }
    }

    fun oppdaterOppgaveSistSjekket(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """update personhendelse set oppgave_sist_sjekket = :oppgave_sist_sjekket where id = :id""",
                    mapOf("oppgave_sist_sjekket" to nå(clock), "id" to id),
                ).asUpdate,
            )
        }
    }

    private fun Row.toPersonhendelseDb(): PersonhendelseDb {
        return PersonhendelseDb(
            id = uuid("id"),
            fnr = Fnr.fromString(string("fnr")),
            hendelseId = string("hendelse_id"),
            opplysningstype = Opplysningstype.valueOf(string("opplysningstype")),
            personhendelseType = objectMapper.readValue(string("personhendelse_type")),
            sakId = SakId.fromString(string("sak_id")),
            oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
            oppgaveSistSjekket = localDateTimeOrNull("oppgave_sist_sjekket"),
        )
    }
}
