package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.LagretEksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

class EksternOppgavePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : EksternOppgaveRepo {
    override fun lagre(eksternOppgave: EksternOppgave, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    INSERT INTO ekstern_oppgave (oppgave_id, sak_id, opprettet, grunnlag, tilleggstekst)
                    VALUES (:oppgave_id, :sak_id, :opprettet, :grunnlag::jsonb, :tilleggstekst)
                    ON CONFLICT (oppgave_id) DO NOTHING
                    """.trimIndent(),
                    "oppgave_id" to eksternOppgave.oppgaveId.toString(),
                    "sak_id" to eksternOppgave.sakId.toString(),
                    "opprettet" to eksternOppgave.opprettet,
                    "grunnlag" to eksternOppgave.grunnlag.toDbJson(),
                    "tilleggstekst" to eksternOppgave.tilleggstekst,
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(sakId: SakId): List<LagretEksternOppgave> =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT oppgave_id, sak_id, opprettet, grunnlag, tilleggstekst
                    FROM ekstern_oppgave
                    WHERE sak_id = :sak_id
                    ORDER BY opprettet, oppgave_id
                    """.trimIndent(),
                    "sak_id" to sakId.toString(),
                ).map { it.toEksternOppgave() }.asList,
            )
        }

    private fun Row.toEksternOppgave() = LagretEksternOppgave(
        oppgaveId = OppgaveId(string("oppgave_id")),
        sakId = SakId.fromString(string("sak_id")),
        opprettet = localDateTime("opprettet"),
        grunnlag = string("grunnlag"),
        tilleggstekst = stringOrNull("tilleggstekst"),
    )
}
