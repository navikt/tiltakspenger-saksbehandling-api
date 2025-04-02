package no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class PersonhendelseRepository(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun hent(fnr: Fnr): List<PersonhendelseDb> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentForFnr, fnr.verdi)
                .map { row -> row.toPersonhendelseDb() }
                .asList,
        )
    }

    fun hentAlleUtenOppgave(): List<PersonhendelseDb> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentAlleUtenOppgave)
                .map { row -> row.toPersonhendelseDb() }
                .asList,
        )
    }

    fun lagre(personhendelseDb: PersonhendelseDb) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    lagrePersonhendelse,
                    mapOf(
                        "id" to personhendelseDb.id,
                        "fnr" to personhendelseDb.fnr.verdi,
                        "hendelse_id" to personhendelseDb.hendelseId,
                        "opplysningstype" to personhendelseDb.opplysningstype.name,
                        "personhendelse_type" to toPGObject(personhendelseDb.personhendelseType),
                        "sak_id" to personhendelseDb.sakId.toString(),
                        "oppgave_id" to personhendelseDb.oppgaveId?.toString(),
                        "sist_oppdatert" to LocalDateTime.now(),
                        "oppgave_sist_sjekket" to personhendelseDb.oppgaveSistSjekket,
                    ),
                ).asUpdate,
            )
        }
    }

    fun slett(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(sqlSlettForId, id).asUpdate,
            )
        }
    }

    fun lagreOppgaveId(id: UUID, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update personhendelse set oppgave_id = :oppgave_id where id = :id
                    """.trimIndent(),
                    mapOf(
                        "oppgave_id" to oppgaveId.toString(),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun Row.toPersonhendelseDb() =
        PersonhendelseDb(
            id = uuid("id"),
            fnr = Fnr.fromString(string("fnr")),
            hendelseId = string("hendelse_id"),
            opplysningstype = Opplysningstype.valueOf(string("opplysningstype")),
            personhendelseType = objectMapper.readValue(string("personhendelse_type")),
            sakId = SakId.fromString(string("sak_id")),
            oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
            oppgaveSistSjekket = localDateTimeOrNull("oppgave_sist_sjekket"),
        )

    @Language("SQL")
    private val lagrePersonhendelse =
        """
        insert into personhendelse (
            id,
            fnr,
            hendelse_id,
            opplysningstype,
            personhendelse_type,
            sak_id,
            oppgave_id,
            sist_oppdatert,
            oppgave_sist_sjekket
        ) values (
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
        """.trimIndent()

    @Language("SQL")
    private val sqlHentForFnr = "select * from personhendelse where fnr = ?"

    @Language("SQL")
    private val sqlHentAlleUtenOppgave = "select * from personhendelse where oppgave_id is null"

    @Language("SQL")
    private val sqlSlettForId = "delete from personhendelse where id = ?"
}
