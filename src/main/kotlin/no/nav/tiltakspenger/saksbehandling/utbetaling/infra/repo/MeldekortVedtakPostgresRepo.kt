package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import java.time.LocalDateTime

internal class MeldekortVedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortVedtakRepo {

    override fun opprett(vedtak: MeldekortVedtak, context: TransactionContext?) {
        sessionFactory.withSession(context) { session ->
            // Må lagre vedtaket og utbetalingen i en transaksjon med deferred constraint pga sirkulære referanser
            session.transaction { tx ->
                tx.run(queryOf("SET CONSTRAINTS meldekortvedtak_utbetaling_id_fkey DEFERRED").asExecute)
                tx.run(
                    sqlQuery(
                        """
                        insert into meldekortvedtak (
                            id,
                            utbetaling_id,
                            sak_id,
                            opprettet,
                            meldekort_id
                        ) values (
                            :id,
                            :utbetaling_id,
                            :sak_id,
                            :opprettet,
                            :meldekort_id
                        )
                    """,
                        "id" to vedtak.id.toString(),
                        "utbetaling_id" to vedtak.utbetaling.id.toString(),
                        "sak_id" to vedtak.sakId.toString(),
                        "opprettet" to vedtak.opprettet,
                        "meldekort_id" to vedtak.utbetaling.beregningKilde.id.toString(),
                    ).asUpdate,
                )
                UtbetalingPostgresRepo.lagre(vedtak.utbetaling, tx)
            }
        }
    }

    override fun markerSendtTilUtbetaling(
        vedtakId: VedtakId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        update meldekortvedtak
                        set sendt_til_utbetaling_tidspunkt = :tidspunkt, 
                            utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """,
                    "id" to vedtakId.toString(),
                    "tidspunkt" to tidspunkt,
                    "metadata" to utbetalingsrespons.toJson(),
                ).asUpdate,
            )
        }
    }

    override fun lagreFeilResponsFraUtbetaling(
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        update meldekortvedtak
                        set utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """,
                    "id" to vedtakId.toString(),
                    "metadata" to utbetalingsrespons.toJson(),
                ).asUpdate,
            )
        }
    }

    override fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        update meldekortvedtak 
                        set journalpost_id = :journalpost_id,
                        journalføringstidspunkt = :tidspunkt
                        where id = :id
                    """,
                    "id" to vedtakId.toString(),
                    "journalpost_id" to journalpostId.toString(),
                    "tidspunkt" to tidspunkt,
                ).asUpdate,
            )
        }
    }

    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String? {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select (utbetaling_metadata->>'request') as req 
                        from meldekortvedtak 
                        where id = :id
                    """,
                    "id" to vedtakId.toString(),
                ).map { row ->
                    row.stringOrNull("req")
                }.asSingle,
            )
        }
    }

    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<MeldekortVedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                            select v.*, u.forrige_utbetaling_vedtak_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
                            from meldekortvedtak v
                            join sak s on s.id = v.sak_id
                            join utbetaling u on u.id = v.utbetaling_id 
                            left join meldekortvedtak parent on parent.id = u.forrige_utbetaling_vedtak_id
                              and parent.sak_id = u.sak_id
                            where u.sendt_til_utbetaling_tidspunkt is null
                              and (u.forrige_utbetaling_vedtak_id is null or (parent.sendt_til_utbetaling_tidspunkt is not null and parent.status IN ('OK','OK_UTEN_UTBETALING')))
                            order by v.opprettet
                            limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }
    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<MeldekortVedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                            select v.*, u.forrige_utbetaling_vedtak_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
                            from meldekortvedtak v
                            join sak s on s.id = v.sak_id
                            join utbetaling u on u.id = v.utbetaling_id 
                            where v.journalpost_id is null
                            limit :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }
    }

    override fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                        update meldekortvedtak
                        set status = :status,
                        status_metadata = to_jsonb(:status_metadata::jsonb)
                        where id = :id
                    """,
                    "id" to vedtakId.toString(),
                    "status" to status.toDbType(),
                    "status_metadata" to metadata.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select v.*, u.forrige_utbetaling_vedtak_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
                        from meldekortvedtak v
                        join sak s on s.id = v.sak_id
                        join utbetaling u on u.id = v.utbetaling_id  
                        where (u.status is null or u.status IN ('IKKE_PÅBEGYNT', 'SENDT_TIL_OPPDRAG')) and u.sendt_til_utbetaling_tidspunkt is not null
                        order by v.opprettet
                        limit :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    UtbetalingDetSkalHentesStatusFor(
                        saksnummer = Saksnummer(row.string("saksnummer")),
                        sakId = SakId.fromString(row.string("sak_id")),
                        vedtakId = VedtakId.fromString(row.string("id")),
                        opprettet = row.localDateTime("opprettet"),
                        sendtTilUtbetalingstidspunkt = row.localDateTime("sendt_til_utbetaling_tidspunkt"),
                        forsøkshistorikk = row.stringOrNull("status_metadata")?.toForsøkshistorikk(),
                    )
                }.asList,
            )
        }
    }

    companion object {
        fun hentForSakId(sakId: SakId, session: Session): MeldekortVedtaksliste {
            return session.run(
                sqlQuery(
                    """
                        select v.*, u.forrige_utbetaling_vedtak_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
                        from meldekortvedtak v
                        join sak s on s.id = v.sak_id
                        join utbetaling u on u.id = v.utbetaling_id 
                        where v.sak_id = :sak_id 
                        order by v.opprettet
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            ).let { MeldekortVedtaksliste(it) }
        }

        private fun Row.toVedtak(session: Session): MeldekortVedtak {
            val vedtakId = VedtakId.fromString(string("id"))
            val sakId = SakId.fromString(string("sak_id"))
            val saksnummer = Saksnummer(string("saksnummer"))
            val fnr = Fnr.fromString(string("fnr"))

            val journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId(it) }
            val journalføringstidspunkt = localDateTimeOrNull("journalføringstidspunkt")
            val opprettet = localDateTime("opprettet")

            val utbetalingId = UtbetalingId.fromString(string("utbetaling_id"))
            val status = stringOrNull("status").toUtbetalingsstatus()
            val sendtTilUtbetaling = localDateTimeOrNull("sendt_til_utbetaling_tidspunkt")
            val forrigeUtbetalingVedtakId = stringOrNull("forrige_utbetaling_vedtak_id")?.let {
                VedtakId.fromString(
                    it,
                )
            }

            val meldekortId = MeldekortId.fromString(string("meldekort_id"))

            val meldekortbehandling = MeldekortBehandlingPostgresRepo
                .hentForMeldekortId(
                    meldekortId,
                    session,
                )

            require(meldekortbehandling is MeldekortBehandling.Behandlet) {
                "Meldekortet $meldekortId på meldekortvedtak $vedtakId er ikke et behandlet meldekort"
            }

            return MeldekortVedtak(
                id = vedtakId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                journalpostId = journalpostId,
                journalføringstidspunkt = journalføringstidspunkt,
                opprettet = opprettet,
                meldekortBehandling = meldekortbehandling,
                utbetaling = Utbetaling(
                    id = utbetalingId,
                    sendtTilUtbetaling = sendtTilUtbetaling,
                    status = status,
                    forrigeUtbetalingVedtakId = forrigeUtbetalingVedtakId,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    beregning = meldekortbehandling.beregning,
                    brukerNavkontor = meldekortbehandling.navkontor,
                    vedtakId = vedtakId,
                    opprettet = opprettet,
                    saksbehandler = meldekortbehandling.saksbehandler!!,
                    beslutter = meldekortbehandling.beslutter!!,
                ),
            )
        }
    }
}
