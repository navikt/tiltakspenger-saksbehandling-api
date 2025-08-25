package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.UtbetalingBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraBehandling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraMeldekort
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import java.time.LocalDateTime

class UtbetalingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingRepo {
    override fun lagre(
        utbetaling: Utbetaling,
        context: TransactionContext?,
    ) {
        sessionFactory.withSession(context) { session ->
            lagre(utbetaling, session)
        }
    }

    override fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        update utbetaling
                        set sendt_til_utbetaling_tidspunkt = :tidspunkt, 
                            utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """,
                    "id" to utbetalingId.toString(),
                    "tidspunkt" to tidspunkt,
                    "metadata" to utbetalingsrespons.toJson(),
                ).asUpdate,
            )
        }
    }

    override fun lagreFeilResponsFraUtbetaling(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        update utbetaling
                        set utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """,
                    "id" to utbetalingId.toString(),
                    "metadata" to utbetalingsrespons.toJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentUtbetalingJson(utbetalingId: UtbetalingId): String? {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select (utbetaling_metadata->>'request') as req 
                        from utbetaling 
                        where id = :id
                    """,
                    "id" to utbetalingId.toString(),
                ).map { it.stringOrNull("req") }.asSingle,
            )
        }
    }

    override fun hentForUtsjekk(limit: Int): List<Utbetaling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                            select v.*, u.forrige_utbetaling_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
                            from meldekortvedtak v
                            join sak s on s.id = v.sak_id
                            join utbetaling u on u.id = v.utbetaling_id 
                            left join utbetaling parent on parent.id = u.forrige_utbetaling_id
                              and parent.sak_id = u.sak_id
                            where u.sendt_til_utbetaling_tidspunkt is null
                              and (u.forrige_utbetaling_id is null or (parent.sendt_til_utbetaling_tidspunkt is not null and parent.status IN ('OK','OK_UTEN_UTBETALING')))
                            order by v.opprettet
                            limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { it.tilUtbetaling() }.asList,
            )
        }
    }

    override fun oppdaterUtbetalingsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                        update utbetaling
                        set status = :status,
                        status_metadata = to_jsonb(:status_metadata::jsonb)
                        where id = :id
                    """,
                    "id" to utbetalingId.toString(),
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
                        select v.*, u.forrige_utbetaling_id, u.status, u.sendt_til_utbetaling_tidspunkt, s.saksnummer, s.fnr 
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
                        utbetalingId = UtbetalingId.fromString(row.string("id")),
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
        fun hent(id: UtbetalingId, session: Session): Utbetaling? {
            return session.run(
                sqlQuery(
                    """
                    select 
                        u.*,
                        u.rammevedtak_id as vedtak_id,
                        u.meldekortvedtak_id as vedtak_id,
                        s.saksnummer,
                        s.fnr,
                        COALESCE(b.beregning, mb.beregninger)
                    from utbetaling u 
                    join sak s on s.id = u.sak_id
                    left join rammevedtak r on u.rammevedtak_id = r.id
                    left join meldekortvedtak m on u.meldekortvedtak_id = m.id
                    left join meldekortbehandling mb on mb.id = m.meldekort_id
                    left join behandling b on b.id = r.behandling_id
                    where u.id = :id 
                """,
                    "id" to id.toString(),
                ).map {
                    it.tilUtbetaling()
                }.asSingle,
            )
        }

        fun lagre(utbetaling: Utbetaling, session: Session) {
            session.run(
                sqlQuery(
                    """
                        insert into utbetaling (
                            id,
                            sak_id,
                            rammevedtak_id,
                            meldekortvedtak_id,
                            forrige_utbetaling_id,
                            sendt_til_utbetaling_tidspunkt,
                            status
                        ) values(
                            :id,
                            :sak_id,
                            :rammevedtak_id,
                            :meldekortvedtak_id,
                            :forrige_utbetaling_id,
                            :sendt_til_utbetaling_tidspunkt,
                            :status
                        )
                    """,
                    "id" to utbetaling.id.toString(),
                    "sak_id" to utbetaling.sakId.toString(),
                    "forrige_utbetaling_id" to utbetaling.forrigeUtbetalingId?.toString(),
                    "sendt_til_utbetaling_tidspunkt" to utbetaling.sendtTilUtbetaling?.toString(),
                    "status" to utbetaling.status.toString(),
                    when (utbetaling.beregningKilde) {
                        is BeregningKilde.Behandling -> "rammevedtak_id" to utbetaling.vedtakId.toString()
                        is BeregningKilde.Meldekort -> "meldekortvedtak_id" to utbetaling.vedtakId.toString()
                    },
                ).asUpdate,
            )
        }

        private fun Row.tilUtbetaling(): Utbetaling {
            val beregningJson = string("beregning")

            val beregning: UtbetalingBeregning = stringOrNull("meldekort_id")?.let {
                val meldekortId = MeldekortId.fromString(it)
                MeldekortBeregning(beregningJson.tilMeldeperiodeBeregningerFraMeldekort(meldekortId))
            } ?: run {
                val behandlingId = BehandlingId.fromString(string("behandling_id"))
                BehandlingBeregning(beregningJson.tilMeldeperiodeBeregningerFraBehandling(behandlingId))
            }

            return Utbetaling(
                id = UtbetalingId.fromString(string("id")),
                vedtakId = VedtakId.fromString(string("vedtak_id")),
                sakId = SakId.fromString(string("sak_id")),
                saksnummer = Saksnummer(string("saksnummer")),
                fnr = Fnr.fromString(string("fnr")),
                brukerNavkontor = Navkontor(
                    kontornummer = string("navkontor"),
                    kontornavn = stringOrNull("navkontor_navn"),
                ),
                opprettet = localDateTime("opprettet"),
                saksbehandler = string("saksbehandler"),
                beslutter = string("beslutter"),
                beregning = beregning,
                forrigeUtbetalingId = stringOrNull("forrige_utbetaling_id")
                    ?.let { UtbetalingId.fromString(it) },
                sendtTilUtbetaling = localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                status = stringOrNull("status")?.toUtbetalingsstatus(),
            )
        }
    }
}
