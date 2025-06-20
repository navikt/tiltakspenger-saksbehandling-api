package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

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
                        behandlet_automatisk_status
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
                        :behandlet_automatisk_status
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
                    "behandlet_automatisk_status" to brukersMeldekort.behandletAutomatiskStatus?.tilDb(),
                ).asUpdate,
            )
        }
    }

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
                    select distinct on (mk.sak_id)
                        mk.*
                    from meldekort_bruker mk
                    join meldeperiode mp on mp.id = mk.meldeperiode_id
                    where behandles_automatisk is true
                    and behandlet_automatisk_status is distinct from :erBehandletStatus
                    order by mk.sak_id, mp.fra_og_med
                    limit 100
                    """,
                    "erBehandletStatus" to BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET.tilDb(),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    override fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: BrukersMeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update meldekort_bruker set
                        behandlet_automatisk_status = :behandlet_automatisk_status,
                        behandles_automatisk = :behandles_automatisk
                    where id = :id
                    """,
                    "id" to meldekortId.toString(),
                    "behandlet_automatisk_status" to status.tilDb(),
                    "behandles_automatisk" to behandlesAutomatisk,
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
                behandletAutomatiskStatus = row.stringOrNull("behandlet_automatisk_status")
                    ?.tilMeldekortBehandletAutomatiskStatus(),
            )
        }
    }
}
