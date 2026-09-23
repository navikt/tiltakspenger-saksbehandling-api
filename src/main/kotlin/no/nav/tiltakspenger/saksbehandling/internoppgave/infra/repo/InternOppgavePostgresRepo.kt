package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Dialoginnlegg
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgave
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Løsningsbegrunnelse

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
                WHERE sak_id = :sak_id AND type = :type AND nøkkel = :nokkel AND løst IS NULL
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
                    WHERE løst IS NULL
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
                        id, sak_id, type, nøkkel, grunnlag, opprettet, sist_endret, versjon, saksbehandler,
                        løsning, behandling_id, begrunnelse, begrunnelse_fritekst, løst, dialog
                    ) VALUES (
                        :id, :sak_id, :type, :nokkel, :grunnlag::jsonb, :opprettet, :sist_endret, :versjon, :saksbehandler,
                        :losning, :behandling_id, :begrunnelse, :begrunnelse_fritekst, :lost, :dialog::jsonb
                    )
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    *oppgave.parametre(),
                    "dialog" to oppgave.dialog.toDbJson(),
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
                        saksbehandler = :saksbehandler, løsning = :losning, behandling_id = :behandling_id,
                        begrunnelse = :begrunnelse, begrunnelse_fritekst = :begrunnelse_fritekst, løst = :lost
                    WHERE id = :id AND versjon = :forventet_versjon AND løst IS NULL
                        AND sak_id = :sak_id AND type = :type AND nøkkel = :nokkel
                    """.trimIndent(),
                    *oppgave.parametre(),
                    "forventet_versjon" to forventetVersjon,
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilDialoginnlegg(id: InternOppgaveId, innlegg: Dialoginnlegg, sessionContext: SessionContext?): Boolean =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    // Radlåsen på oppdateringen gjør at et innlegg aldri kommer inn etter at en samtidig løsning er lagret.
                    """
                    UPDATE intern_oppgave
                    SET dialog = dialog || :innlegg::jsonb
                    WHERE id = :id AND løst IS NULL
                    """.trimIndent(),
                    "id" to id.toString(),
                    "innlegg" to listOf(innlegg).toDbJson(),
                ).asUpdate,
            ) > 0
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
        "losning" to løsning?.let { it.utfall.toDb().name },
        "behandling_id" to (løsning?.utfall as? InternOppgaveløsning.Utfall.Revurdering)?.let { it.behandlingId.toString() },
        "begrunnelse" to (løsning?.begrunnelse as? Løsningsbegrunnelse.Forhåndsdefinert)?.let { it.årsak.toDb().name },
        "begrunnelse_fritekst" to (løsning?.begrunnelse as? Løsningsbegrunnelse.Fritekst)?.let { it.tekst.value },
        "lost" to løsning?.løst,
    )

    private fun Row.tilOppgave() = InternOppgave(
        id = InternOppgaveId.fromString(string("id")),
        sakId = SakId.fromString(string("sak_id")),
        grunnlag = InternOppgavetypeDb.valueOf(string("type")).tilGrunnlag(string("nøkkel"), string("grunnlag")),
        opprettet = localDateTime("opprettet"),
        sistEndret = localDateTime("sist_endret"),
        versjon = long("versjon"),
        saksbehandler = stringOrNull("saksbehandler"),
        løsning = stringOrNull("løsning")?.let {
            InternOppgaveløsning(
                utfall = InternOppgaveløsningDb.valueOf(it).tilUtfall(stringOrNull("behandling_id")),
                begrunnelse = tilLøsningsbegrunnelse(stringOrNull("begrunnelse"), stringOrNull("begrunnelse_fritekst")),
                // intern_oppgave_løsning krever løst når løsning er satt.
                løst = localDateTime("løst"),
            )
        },
        dialog = string("dialog").tilDialog(),
    )
}
