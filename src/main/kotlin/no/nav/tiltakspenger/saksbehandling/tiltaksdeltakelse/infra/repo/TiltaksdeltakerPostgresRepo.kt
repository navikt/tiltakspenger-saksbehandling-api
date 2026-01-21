package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb.TiltaksdeltakerIdOgTiltakstype

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
        tiltakstype: TiltakResponsDTO.TiltakType,
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
                                ekstern_id,
                                tiltakstype
                            ) values (
                                :id,
                                :ekstern_id,
                                :tiltakstype
                            )
                            """.trimIndent(),
                            "id" to id.toString(),
                            "ekstern_id" to eksternId,
                            "tiltakstype" to tiltakstype.name,
                        ).asUpdate,
                    )
                    return@withSession id
                }
            }
        }
    }

    override fun hentEksternId(id: TiltaksdeltakerId): String {
        return sessionFactory.withSession { session ->
            hentEksternId(id, session)
        }
    }

    override fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakType,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSessionContext(sessionContext) { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            insert into tiltaksdeltaker (
                                id,
                                ekstern_id,
                                tiltakstype
                            ) values (
                                :id,
                                :ekstern_id,
                                :tiltakstype
                            )
                        """.trimIndent(),
                        "id" to id.toString(),
                        "ekstern_id" to eksternId,
                        "tiltakstype" to tiltakstype.name,
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

    override fun hentIdUtenTiltakstypeOgTiltakstypen(limit: Int): List<TiltaksdeltakerIdOgTiltakstype> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select t.id as tiltaksdeltaker_id, s.typekode as tiltakstype
                        from tiltaksdeltaker t
                                 inner join søknadstiltak s on t.id = s.tiltaksdeltaker_id
                        where t.tiltakstype is null
                        limit :limit;
                    """.trimIndent(),
                    "limit" to limit,
                )
                    .map { row -> row.tilTiltaksdeltakerIdOgTiltakstype() }
                    .asList,
            )
        }
    }

    override fun lagreTiltakstype(tiltaksdeltakerIdOgTiltakstype: TiltaksdeltakerIdOgTiltakstype) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                            update tiltaksdeltaker
                                set tiltakstype = :tiltakstype
                            where id = :id and tiltakstype is null
                    """.trimIndent(),
                    "tiltakstype" to tiltaksdeltakerIdOgTiltakstype.tiltakstype.name,
                    "id" to tiltaksdeltakerIdOgTiltakstype.id.toString(),
                ).asUpdate,
            )
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

    private fun Row.tilTiltaksdeltakerIdOgTiltakstype() =
        TiltaksdeltakerIdOgTiltakstype(
            id = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
            tiltakstype = TiltakResponsDTO.TiltakType.valueOf(string("tiltakstype")),
        )
}
