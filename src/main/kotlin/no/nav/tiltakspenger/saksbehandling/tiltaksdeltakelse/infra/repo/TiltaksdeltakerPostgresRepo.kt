package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

class TiltaksdeltakerPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : TiltaksdeltakerRepo {

    companion object {
        fun hentEksternId(
            internId: TiltaksdeltakerId,
            session: Session,
        ): String {
            return session.run(
                queryOf(
                    "select ekstern_id from tiltaksdeltaker where id = ?",
                    internId.toString(),
                )
                    .map { row -> row.string("ekstern_id") }
                    .asSingle,
            ) ?: throw IllegalArgumentException("Fant ikke eksternId for internId $internId!")
        }
    }

    override fun hentEllerLagre(
        eksternId: String,
        sessionContext: SessionContext?,
    ): TiltaksdeltakerId {
        return sessionFactory.withSessionContext(sessionContext) { sessionContext ->
            sessionContext.withSession { session ->
                val id = hentForEksternId(eksternId, session)
                if (id != null) {
                    return@withSession id
                } else {
                    val id = TiltaksdeltakerId.random()
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
                            "id" to id.toString(),
                            "ekstern_id" to eksternId,
                        ).asUpdate,
                    )
                    return@withSession id
                }
            }
        }
    }

    override fun lagre(id: TiltaksdeltakerId, eksternId: String, sessionContext: SessionContext?) {
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
                        "id" to id.toString(),
                        "ekstern_id" to eksternId,
                    ).asUpdate,
                )
            }
        }
    }

    override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
        return sessionFactory.withSession { session ->
            hentForEksternId(eksternId, session)
        }
    }

    private fun hentForEksternId(
        eksternId: String,
        session: Session,
    ): TiltaksdeltakerId? = session.run(
        queryOf(
            "select id from tiltaksdeltaker where ekstern_id = ?",
            eksternId,
        )
            .map { row -> row.stringOrNull("id")?.let { TiltaksdeltakerId.fromString(it) } }
            .asSingle,
    )
}
