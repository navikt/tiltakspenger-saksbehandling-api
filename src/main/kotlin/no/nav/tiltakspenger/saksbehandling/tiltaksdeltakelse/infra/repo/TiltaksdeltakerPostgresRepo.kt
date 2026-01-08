package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery

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
                    val id = random(ULID_PREFIX_TILTAKSDELTAKER).toString()
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

    override fun lagre(id: String, eksternId: String, sessionContext: SessionContext?) {
        sessionFactory.withSessionContext(sessionContext) { sessionContext ->
            sessionContext.withSession { session ->
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
