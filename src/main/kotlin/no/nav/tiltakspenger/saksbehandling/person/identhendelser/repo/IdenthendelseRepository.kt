package no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class IdenthendelseRepository(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun lagre(identhendelseDb: IdenthendelseDb) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    lagreIdenthendelse,
                    mapOf(
                        "id" to identhendelseDb.id,
                        "gammelt_fnr" to identhendelseDb.gammeltFnr.verdi,
                        "nytt_fnr" to identhendelseDb.nyttFnr.verdi,
                        "personidenter" to toPGObject(identhendelseDb.personidenter),
                        "sak_id" to identhendelseDb.sakId.toString(),
                        "produsert_hendelse" to identhendelseDb.produsertHendelse,
                        "oppdatert_database" to identhendelseDb.oppdatertDatabase,
                        "sist_oppdatert" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }

    fun hent(gammeltFnr: Fnr): List<IdenthendelseDb> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentForGammeltFnr, gammeltFnr.verdi)
                .map { row -> row.toIdenthendelseDb() }
                .asList,
        )
    }

    fun hent(id: UUID): IdenthendelseDb? = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentForId, id)
                .map { row -> row.toIdenthendelseDb() }
                .asSingle,
        )
    }

    fun hentAlleSomIkkeErBehandlet(): List<IdenthendelseDb> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentAlleSomIkkeErBehandlet)
                .map { row -> row.toIdenthendelseDb() }
                .asList,
        )
    }

    fun oppdaterProdusertHendelse(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update identhendelse set produsert_hendelse = :produsert_hendelse where id = :id
                    """.trimIndent(),
                    mapOf(
                        "produsert_hendelse" to LocalDateTime.now(),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

    fun oppdaterOppdatertDatabase(id: UUID) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update identhendelse set oppdatert_database = :oppdatert_database where id = :id
                    """.trimIndent(),
                    mapOf(
                        "oppdatert_database" to LocalDateTime.now(),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun Row.toIdenthendelseDb() =
        IdenthendelseDb(
            id = uuid("id"),
            gammeltFnr = Fnr.fromString(string("gammelt_fnr")),
            nyttFnr = Fnr.fromString(string("nytt_fnr")),
            personidenter = objectMapper.readValue(string("personidenter")),
            sakId = SakId.fromString(string("sak_id")),
            produsertHendelse = localDateTimeOrNull("produsert_hendelse"),
            oppdatertDatabase = localDateTimeOrNull("oppdatert_database"),
        )

    @Language("SQL")
    private val lagreIdenthendelse =
        """
        insert into identhendelse (
            id,
            gammelt_fnr,
            nytt_fnr,
            personidenter,
            sak_id,
            produsert_hendelse,
            oppdatert_database
        ) values (
            :id,
            :gammelt_fnr,
            :nytt_fnr,
            :personidenter,
            :sak_id,
            :produsert_hendelse,
            :oppdatert_database
        )
        """.trimIndent()

    @Language("SQL")
    private val sqlHentForGammeltFnr = "select * from identhendelse where gammelt_fnr = ?"

    @Language("SQL")
    private val sqlHentForId = "select * from identhendelse where id = ?"

    @Language("SQL")
    private val sqlHentAlleSomIkkeErBehandlet = "select * from identhendelse where produsert_hendelse is null or oppdatert_database is null"
}
