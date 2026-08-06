package no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.Personhendelse
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseMedOppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseRepo
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class PersonhendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : PersonhendelseRepo {
    override fun hent(sakId: SakId): List<Personhendelse> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """select * from personhendelse where sak_id = :sak_id""",
                    mapOf("sak_id" to sakId.toString()),
                ).map { row -> row.toPersonhendelseDb().toDomain() }.asList,
            )
        }
    }

    override fun hent(id: UUID): Personhendelse? {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """select * from personhendelse where id = :id""",
                    mapOf("id" to id),
                ).map { row -> row.toPersonhendelseDb().toDomain() }.asSingle,
            )
        }
    }

    override fun hentMedOppgaveId(id: UUID): PersonhendelseMedOppgaveId? {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        select hendelse_id, oppgave_id
                        from personhendelse
                        where id = :id
                          and oppgave_id is not null
                    """.trimIndent(),
                    mapOf("id" to id),
                ).map { row ->
                    PersonhendelseMedOppgaveId(
                        hendelseId = row.string("hendelse_id"),
                        oppgaveId = OppgaveId(row.string("oppgave_id")),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentIderUtenOppgave(): List<UUID> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """select id from personhendelse where oppgave_id is null""",
                ).map { row -> row.uuid("id") }.asList,
            )
        }
    }

    override fun hentIderMedOppgave(
        oppgaveSistSjekket: LocalDateTime,
    ): List<UUID> {
        return sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        select id
                        from personhendelse
                        where oppgave_id is not null
                          and (oppgave_sist_sjekket is null or oppgave_sist_sjekket < :oppgave_sist_sjekket)
                    """.trimIndent(),
                    mapOf(
                        "oppgave_sist_sjekket" to oppgaveSistSjekket,
                    ),
                ).map { row -> row.uuid("id") }.asList,
            )
        }
    }

    override fun lagre(personhendelse: Personhendelse) {
        val personhendelseDb = personhendelse.toDb()
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
                            sist_oppdatert
                        ) VALUES (
                            :id,
                            :fnr,
                            :hendelse_id,
                            :opplysningstype,
                            :personhendelse_type::jsonb,
                            :sak_id,
                            :sist_oppdatert
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to personhendelseDb.id,
                        "fnr" to personhendelseDb.fnr,
                        "hendelse_id" to personhendelseDb.hendelseId,
                        "opplysningstype" to personhendelseDb.opplysningstype,
                        "personhendelse_type" to personhendelseDb.personhendelseType,
                        "sak_id" to personhendelseDb.sakId,
                        "sist_oppdatert" to nå(clock),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun slett(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """delete from personhendelse where id = :id""",
                    mapOf("id" to id),
                ).asUpdate,
            )
        }
    }

    override fun lagreOppgaveId(id: UUID, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """update personhendelse set oppgave_id = :oppgave_id where id = :id""",
                    mapOf("oppgave_id" to oppgaveId.toString(), "id" to id),
                ).asUpdate,
            )
        }
    }

    override fun oppdaterOppgaveSistSjekket(id: UUID) {
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
            fnr = string("fnr"),
            hendelseId = string("hendelse_id"),
            opplysningstype = string("opplysningstype"),
            personhendelseType = string("personhendelse_type"),
            sakId = string("sak_id"),
        )
    }
}

private data class PersonhendelseDb(
    val id: UUID,
    val fnr: String,
    val hendelseId: String,
    val opplysningstype: String,
    val personhendelseType: String,
    val sakId: String,
) {
    fun toDomain() = Personhendelse(
        id = id,
        fnr = Fnr.fromString(fnr),
        hendelseId = hendelseId,
        opplysningstype = Opplysningstype.valueOf(opplysningstype),
        personhendelseType = personhendelseType.fromDbJsonToPersonhendelseType(),
        sakId = SakId.fromString(sakId),
    )
}

private fun Personhendelse.toDb() = PersonhendelseDb(
    id = id,
    fnr = fnr.verdi,
    hendelseId = hendelseId,
    opplysningstype = opplysningstype.name,
    personhendelseType = personhendelseType.toDbJson(),
    sakId = sakId.toString(),
)
