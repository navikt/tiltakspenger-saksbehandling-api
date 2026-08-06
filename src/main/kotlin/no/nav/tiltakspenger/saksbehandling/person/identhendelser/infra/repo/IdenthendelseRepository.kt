package no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import org.intellij.lang.annotations.Language
import java.time.Clock
import java.util.UUID

// TODO: Klassen står i whitelisten til RepoKonvensjonKonsistTest fordi suffikset skal være `PostgresRepo`, ikke `Repository`.
//  Unntaket er ikke målet, men omdøpingen alene gjør ikke nytte: den slår på interface-kravet i samme regel.
//  Porten kan ikke lages før vertikalen har en domenetype — repoet snakker `IdenthendelseDb` i dag — så ta de to sammen.
class IdenthendelseRepository(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
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
                        "personidenter" to identhendelseDb.personidenter.toDbJson(),
                        "sak_id" to identhendelseDb.sakId.toString(),
                        "produsert_hendelse" to identhendelseDb.produsertHendelse,
                        "oppdatert_database" to identhendelseDb.oppdatertDatabase,
                        "sist_oppdatert" to nå(clock),
                    ),
                ).asUpdate,
            )
        }
    }

    fun hent(id: UUID): IdenthendelseDb? = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentForId, id)
                .map { row -> row.toIdenthendelseDb() }
                .asSingle,
        )
    }

    fun hentIderSomIkkeErBehandlet(): List<UUID> = sessionFactory.withSession {
        it.run(
            queryOf(sqlHentIderSomIkkeErBehandlet)
                .map { row -> row.uuid("id") }
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
                        "produsert_hendelse" to nå(clock),
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
                        "oppdatert_database" to nå(clock),
                        "id" to id,
                    ),
                ).asUpdate,
            )
        }
    }

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
            :personidenter::jsonb,
            :sak_id,
            :produsert_hendelse,
            :oppdatert_database
        )
        """.trimIndent()

    @Language("SQL")
    private val sqlHentForId = "select * from identhendelse where id = ?"

    @Language("SQL")
    private val sqlHentIderSomIkkeErBehandlet =
        "select id from identhendelse where produsert_hendelse is null or oppdatert_database is null"
}

fun Row.toIdenthendelseDb() =
    IdenthendelseDb(
        id = uuid("id"),
        gammeltFnr = Fnr.fromString(string("gammelt_fnr")),
        nyttFnr = Fnr.fromString(string("nytt_fnr")),
        personidenter = string("personidenter").fromDbJsonToPersonidenter(),
        sakId = SakId.fromString(string("sak_id")),
        produsertHendelse = localDateTimeOrNull("produsert_hendelse"),
        oppdatertDatabase = localDateTimeOrNull("oppdatert_database"),
    )
