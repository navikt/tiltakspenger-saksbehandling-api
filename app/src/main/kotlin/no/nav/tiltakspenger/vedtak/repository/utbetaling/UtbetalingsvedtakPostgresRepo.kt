package no.nav.tiltakspenger.vedtak.repository.utbetaling

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.MeldekortBehandlet
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortBehandlingPostgresRepo
import java.time.LocalDateTime

internal class UtbetalingsvedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingsvedtakRepo {
    override fun lagre(vedtak: Utbetalingsvedtak, context: TransactionContext?) {
        sessionFactory.withSession(context) { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        insert into utbetalingsvedtak (
                            id,
                            sak_id,
                            opprettet,
                            forrige_vedtak_id,
                            meldekort_id
                        ) values (
                            :id,
                            :sak_id,
                            :opprettet,
                            :forrige_vedtak_id,
                            :meldekort_id
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtak.id.toString(),
                        "sak_id" to vedtak.sakId.toString(),
                        "opprettet" to vedtak.opprettet,
                        "forrige_vedtak_id" to vedtak.forrigeUtbetalingsvedtakId?.toString(),
                        "meldekort_id" to vedtak.meldekortId.toString(),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun markerSendtTilUtbetaling(
        vedtakId: VedtakId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        update utbetalingsvedtak
                        set sendt_til_utbetaling_tidspunkt = :tidspunkt, 
                            utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtakId.toString(),
                        "tidspunkt" to tidspunkt,
                        "metadata" to utbetalingsrespons.toJson(),
                    ),
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
                queryOf(
                    //language=SQL
                    """
                        update utbetalingsvedtak 
                        set journalpost_id = :journalpost_id,
                        journalføringstidspunkt = :tidspunkt
                        where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtakId.toString(),
                        "journalpost_id" to journalpostId.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        select (utbetaling_metadata->>'request') as req 
                        from utbetalingsvedtak 
                        where id = :id
                    """.trimIndent(),
                    mapOf("id" to vedtakId.toString()),
                ).map { row ->
                    row.stringOrNull("req")
                }.asSingle,
            )
        }
    }

    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<Utbetalingsvedtak> =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        select u.*, s.fnr, s.saksnummer
                        from utbetalingsvedtak u
                        join sak s on s.id = u.sak_id
                        left join utbetalingsvedtak parent on parent.id = u.forrige_vedtak_id
                          and parent.sak_id = u.sak_id
                        where u.sendt_til_utbetaling_tidspunkt is null
                          and (u.forrige_vedtak_id is null or parent.sendt_til_utbetaling_tidspunkt is not null)
                        order by u.opprettet
                        limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }

    override fun hentDeSomSkalJournalføres(limit: Int): List<Utbetalingsvedtak> =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        select u.*, s.fnr, s.saksnummer 
                        from utbetalingsvedtak u 
                        join sak s on s.id = u.sak_id 
                        where u.journalpost_id is null
                        limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }

    companion object {
        fun hentForSakId(sakId: SakId, session: Session): Utbetalinger {
            return session.run(
                queryOf(
                    //language=SQL
                    """
                        select u.*, s.saksnummer, s.fnr 
                        from utbetalingsvedtak u 
                        join sak s on s.id = u.sak_id 
                        where u.sak_id = :sak_id 
                        order by u.opprettet
                    """.trimIndent(),
                    mapOf("sak_id" to sakId.toString()),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            ).let { Utbetalinger(it) }
        }

        private fun Row.toVedtak(session: Session): Utbetalingsvedtak {
            val vedtakId = VedtakId.fromString(string("id"))
            return Utbetalingsvedtak(
                id = vedtakId,
                sakId = SakId.fromString(string("sak_id")),
                saksnummer = Saksnummer(string("saksnummer")),
                fnr = Fnr.fromString(string("fnr")),
                forrigeUtbetalingsvedtakId = stringOrNull("forrige_vedtak_id")?.let { VedtakId.fromString(it) },
                meldekortbehandling =
                MeldekortBehandlingPostgresRepo
                    .hentForMeldekortId(
                        MeldekortId.fromString(string("meldekort_id")),
                        session,
                    )!! as MeldekortBehandlet,
                sendtTilUtbetaling = localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId(it) },
                journalføringstidspunkt = localDateTimeOrNull("journalføringstidspunkt"),
                opprettet = localDateTime("opprettet"),
            )
        }
    }
}
