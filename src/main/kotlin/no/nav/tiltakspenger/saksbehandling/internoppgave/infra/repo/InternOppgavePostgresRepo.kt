package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgave
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype

class InternOppgavePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : InternOppgaveRepo {
    override fun hent(id: InternOppgaveId, sessionContext: SessionContext?): InternOppgave? =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT * FROM intern_oppgave WHERE id = :id
                    """.trimIndent(),
                    "id" to id.toString(),
                ).map { it.tilOppgave() }.asSingle,
            )
        }

    override fun hentÅpen(
        sakId: SakId,
        type: InternOppgavetype,
        nøkkel: String,
        sessionContext: SessionContext?,
    ): InternOppgave? = sessionFactory.withSession(sessionContext) { session ->
        session.run(
            sqlQuery(
                """
                SELECT * FROM intern_oppgave
                WHERE sak_id = :sak_id AND type = :type AND nokkel = :nokkel AND lost IS NULL
                """.trimIndent(),
                "sak_id" to sakId.toString(),
                "type" to type.toDb().name,
                "nokkel" to nøkkel,
            ).map { it.tilOppgave() }.asSingle,
        )
    }

    override fun hentForSak(sakId: SakId, sessionContext: SessionContext?): List<InternOppgave> =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT * FROM intern_oppgave
                    WHERE sak_id = :sak_id
                    ORDER BY opprettet, id
                    """.trimIndent(),
                    "sak_id" to sakId.toString(),
                ).map { it.tilOppgave() }.asList,
            )
        }

    override fun hentUløste(limit: Int, offset: Int, sessionContext: SessionContext?): List<InternOppgave> {
        require(limit in 1..100 && offset >= 0)
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT * FROM intern_oppgave
                    WHERE lost IS NULL
                    ORDER BY opprettet, id
                    LIMIT :limit OFFSET :offset
                    """.trimIndent(),
                    "limit" to limit,
                    "offset" to offset,
                ).map { it.tilOppgave() }.asList,
            )
        }
    }

    override fun opprett(oppgave: InternOppgave, sessionContext: SessionContext?): Boolean =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    INSERT INTO intern_oppgave (
                        id, sak_id, type, nokkel, grunnlag, opprettet, sist_endret,
                        versjon, saksbehandler, losning, behandling_id, lost
                    ) VALUES (
                        :id, :sak_id, :type, :nokkel, :grunnlag::jsonb, :opprettet, :sist_endret,
                        :versjon, :saksbehandler, :losning, :behandling_id, :lost
                    )
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    *oppgave.parametre(),
                ).asUpdate,
            ) > 0
        }

    override fun oppdater(oppgave: InternOppgave, forventetVersjon: Long, sessionContext: SessionContext?): Boolean {
        require(oppgave.versjon == forventetVersjon + 1)
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE intern_oppgave
                    SET grunnlag = :grunnlag::jsonb, sist_endret = :sist_endret, versjon = :versjon,
                        saksbehandler = :saksbehandler, losning = :losning,
                        behandling_id = :behandling_id, lost = :lost
                    WHERE id = :id AND versjon = :forventet_versjon AND lost IS NULL
                        AND sak_id = :sak_id AND type = :type AND nokkel = :nokkel
                    """.trimIndent(),
                    *oppgave.parametre(),
                    "forventet_versjon" to forventetVersjon,
                ).asUpdate,
            ) > 0
        }
    }

    private fun InternOppgave.parametre() = arrayOf(
        "id" to id.toString(),
        "sak_id" to sakId.toString(),
        "type" to grunnlag.type.toDb().name,
        "nokkel" to grunnlag.nøkkel,
        "grunnlag" to grunnlag.toDbJson(),
        "opprettet" to opprettet,
        "sist_endret" to sistEndret,
        "versjon" to versjon,
        "saksbehandler" to saksbehandler,
        "losning" to løsning?.let { it.toDb().name },
        "behandling_id" to (løsning as? InternOppgaveløsning.Revurdering)?.let { it.behandlingId.toString() },
        "lost" to løst,
    )

    private fun Row.tilOppgave() = InternOppgave(
        id = InternOppgaveId.fromString(string("id")),
        sakId = SakId.fromString(string("sak_id")),
        grunnlag = InternOppgavetypeDb.valueOf(string("type")).tilGrunnlag(string("nokkel"), string("grunnlag")),
        opprettet = localDateTime("opprettet"),
        sistEndret = localDateTime("sist_endret"),
        versjon = long("versjon"),
        saksbehandler = stringOrNull("saksbehandler"),
        løsning = stringOrNull("losning")?.let { InternOppgaveløsningDb.valueOf(it).tilDomene(stringOrNull("behandling_id")) },
        løst = localDateTimeOrNull("lost"),
    )
}
