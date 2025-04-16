package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.time.LocalDateTime

class BrukersMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BrukersMeldekortRepo {
    override fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into meldekort_bruker (
                        id,
                        meldeperiode_id,
                        meldeperiode_kjede_id,
                        meldeperiode_versjon,
                        sak_id,
                        mottatt,
                        dager,
                        journalpost_id,
                        oppgave_id,
                        behandles_automatisk,
                        behandlet_tidspunkt
                    ) values (
                        :id,
                        :meldeperiode_id,
                        (SELECT kjede_id FROM meldeperiode WHERE id = :meldeperiode_id),
                        (SELECT versjon FROM meldeperiode WHERE id = :meldeperiode_id),
                        :sak_id,
                        :mottatt,
                        to_jsonb(:dager::jsonb),
                        :journalpost_id,
                        :oppgave_id,
                        :behandles_automatisk,
                        :behandlet_tidspunkt
                    )
                    """,
                    "id" to brukersMeldekort.id.toString(),
                    "meldeperiode_id" to brukersMeldekort.meldeperiodeId.toString(),
                    "sak_id" to brukersMeldekort.sakId.toString(),
                    "mottatt" to brukersMeldekort.mottatt,
                    "dager" to brukersMeldekort.dager.toDbJson(),
                    "journalpost_id" to brukersMeldekort.journalpostId.toString(),
                    "oppgave_id" to brukersMeldekort.oppgaveId?.toString(),
                    "behandles_automatisk" to brukersMeldekort.behandlesAutomatisk,
                    "behandlet_tidspunkt" to brukersMeldekort.behandletTidspunkt,
                ).asUpdate,
            )
        }
    }

    /**
     * Oppdaterer et meldekort som allerede er lagret i databasen.
     */
    override fun oppdaterOppgaveId(
        meldekortId: MeldekortId,
        oppgaveId: OppgaveId,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                update meldekort_bruker 
                    set oppgave_id = :oppgave_id
                where id = :id
                """,
                    "id" to meldekortId.toString(),
                    "oppgave_id" to oppgaveId.toString(),
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, sessionContext: SessionContext?): BrukersMeldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForMeldekortId(meldekortId, session)
        }
    }

    override fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForMeldeperiodeId(meldeperiodeId, session)
        }
    }

    override fun hentMeldekortSomSkalBehandlesAutomatisk(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select *
                        from meldekort_bruker
                    where behandles_automatisk is true
                    and behandlet_tidspunkt is null
                    """,
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    override fun markerMeldekortSomBehandlet(
        meldekortId: MeldekortId,
        behandletTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                update meldekort_bruker 
                    set behandlet_tidspunkt = :behandlet_tidspunkt
                where id = :id
                """,
                    "id" to meldekortId.toString(),
                    "behandlet_tidspunkt" to behandletTidspunkt,
                ).asUpdate,
            )
        }
    }

    override fun markerMeldekortSomIkkeAutomatiskBehandlet(meldekortId: MeldekortId, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                update meldekort_bruker 
                    set behandles_automatisk = false
                where id = :id
                """,
                    "id" to meldekortId.toString(),
                ).asUpdate,
            )
        }
    }

    override fun hentMeldekortSomDetSkalOpprettesOppgaveFor(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select *
                        from meldekort_bruker 
                    where journalpost_id is not null
                    and oppgave_id is null
                    and behandles_automatisk is false
                    """,
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    companion object {
        fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): List<BrukersMeldekort> {
            return session.run(
                sqlQuery(
                    """
                    select m.*
                        from meldekort_bruker m 
                    where m.sak_id = :sak_id
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }

        fun hentForMeldekortId(
            meldekortId: MeldekortId,
            session: Session,
        ): BrukersMeldekort? {
            return session.run(
                sqlQuery(
                    """
                    select m.*
                        from meldekort_bruker m 
                    where m.id = :id
                    """,
                    "id" to meldekortId.toString(),
                ).map { row -> fromRow(row, session) }.asSingle,
            )
        }

        fun hentForMeldeperiodeId(
            meldeperiodeId: MeldeperiodeId,
            session: Session,
        ): BrukersMeldekort? {
            return session.run(
                sqlQuery(
                    """
                    select m.*
                        from meldekort_bruker m 
                    where m.meldeperiode_id = :meldeperiode_id
                    """,
                    "meldeperiode_id" to meldeperiodeId.toString(),
                ).map { row -> fromRow(row, session) }.asSingle,
            )
        }

        private fun fromRow(
            row: Row,
            session: Session,
        ): BrukersMeldekort {
            return BrukersMeldekort(
                id = MeldekortId.fromString(row.string("id")),
                mottatt = row.localDateTime("mottatt"),
                meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(
                    MeldeperiodeId.fromString(row.string("meldeperiode_id")),
                    session,
                )!!,
                sakId = SakId.fromString(row.string("sak_id")),
                dager = row.string("dager").toMeldekortDager(),
                journalpostId = JournalpostId(row.string("journalpost_id")),
                oppgaveId = row.stringOrNull("oppgave_id")?.let { OppgaveId(it) },
                behandlesAutomatisk = row.boolean("behandles_automatisk"),
                behandletTidspunkt = row.localDateTimeOrNull("behandlet_tidspunkt"),
            )
        }
    }
}
