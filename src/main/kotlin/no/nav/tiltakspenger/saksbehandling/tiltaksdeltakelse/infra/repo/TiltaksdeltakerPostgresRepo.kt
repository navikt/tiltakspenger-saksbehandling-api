package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import java.util.UUID

class TiltaksdeltakerPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : TiltaksdeltakerRepo {

    override fun hentEllerLagre(
        eksternId: String,
        sessionContext: SessionContext?,
    ): String {
        return sessionFactory.withSessionContext(sessionContext) { sessionContext ->
            sessionContext.withSession { session ->
                val id = hentForEksternId(eksternId, session)
                if (id != null) {
                    return@withSession id
                } else {
                    val id = UUID.randomUUID().toString()
                    session.run(
                        sqlQuery(
                            """
                            insert into tiltaksdeltaker (
                                id,
                                ekstern_id
                            ) values (
                                :id,
                                :ekstern_id
                            )
                            """.trimIndent(),
                            "id" to id,
                            "ekstern_id" to eksternId,
                        ).asUpdate,
                    )
                    return@withSession id
                }
            }
        }
    }

    override fun hentInternId(eksternId: String): String? {
        return sessionFactory.withSession { session ->
            hentForEksternId(eksternId, session)
        }
    }

    private fun hentForEksternId(
        eksternId: String,
        session: Session,
    ): String? = session.run(
        queryOf(
            "select id from tiltaksdeltaker where ekstern_id = ?",
            eksternId,
        )
            .map { row -> row.string("id") }
            .asSingle,
    )
}
