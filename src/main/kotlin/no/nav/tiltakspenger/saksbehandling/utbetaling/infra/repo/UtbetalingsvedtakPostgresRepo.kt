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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
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
                            meldekort_id,
                            behandling_id
                        ) values (
                            :id,
                            :sak_id,
                            :opprettet,
                            :forrige_vedtak_id,
                            :meldekort_id,
                            :behandling_id
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtak.id.toString(),
                        "sak_id" to vedtak.sakId.toString(),
                        "opprettet" to vedtak.opprettet,
                        "forrige_vedtak_id" to vedtak.forrigeUtbetalingsvedtakId?.toString(),
                        when (vedtak.beregningKilde) {
                            is BeregningKilde.Behandling -> "behandling_id" to vedtak.beregningKilde.id.toString()
                            is BeregningKilde.Meldekort -> "meldekort_id" to vedtak.beregningKilde.id.toString()
                        },
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

    override fun lagreFeilResponsFraUtbetaling(
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        update utbetalingsvedtak
                        set utbetaling_metadata = to_jsonb(:metadata::jsonb)
                        where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtakId.toString(),
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

    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<Utbetalingsvedtak> {
        return sessionFactory.withSession { session ->
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
                              and (u.forrige_vedtak_id is null or (parent.sendt_til_utbetaling_tidspunkt is not null and parent.status IN ('OK','OK_UTEN_UTBETALING')))
                            order by u.opprettet
                            limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }
    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<Utbetalingsvedtak> {
        return sessionFactory.withSession { session ->
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
    }

    override fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        sessionFactory.withSession(context) { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                        update utbetalingsvedtak
                        set status = :status,
                        status_metadata = to_jsonb(:status_metadata::jsonb)
                        where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtakId.toString(),
                        "status" to status.toDbType(),
                        "status_metadata" to metadata.toDbJson(),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                            select u.id, u.sak_id, u.opprettet, u.sendt_til_utbetaling_tidspunkt, u.status_metadata, s.saksnummer 
                            from utbetalingsvedtak u 
                            join sak s on s.id = u.sak_id 
                            where (u.status is null or u.status IN ('IKKE_PÅBEGYNT', 'SENDT_TIL_OPPDRAG')) and u.sendt_til_utbetaling_tidspunkt is not null
                            order by u.opprettet
                            limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
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
            val sakId = SakId.fromString(string("sak_id"))
            val saksnummer = Saksnummer(string("saksnummer"))
            val fnr = Fnr.fromString(string("fnr"))
            val forrigeUtbetalingsvedtakId = stringOrNull("forrige_vedtak_id")?.let {
                VedtakId.fromString(
                    it,
                )
            }
            val sendtTilUtbetaling = localDateTimeOrNull("sendt_til_utbetaling_tidspunkt")
            val journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId(it) }
            val journalføringstidspunkt = localDateTimeOrNull("journalføringstidspunkt")
            val opprettet = localDateTime("opprettet")
            val status = stringOrNull("status").toUtbetalingsstatus()

            val beregningKilde = BeregningKildeDb.valueOf(string("beregning_kilde"))

            return when (beregningKilde) {
                BeregningKildeDb.MELDEKORT -> {
                    val meldekortId = MeldekortId.fromString(string("meldekort_id"))

                    val meldekortbehandling = MeldekortBehandlingPostgresRepo
                        .hentForMeldekortId(
                            meldekortId,
                            session,
                        )

                    require(meldekortbehandling is MeldekortBehandling.Behandlet) {
                        "Meldekortet $meldekortId på utbetalingsvedtak $vedtakId er ikke et behandlet meldekort"
                    }

                    Utbetalingsvedtak(
                        id = vedtakId,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtakId,
                        sendtTilUtbetaling = sendtTilUtbetaling,
                        journalpostId = journalpostId,
                        journalføringstidspunkt = journalføringstidspunkt,
                        opprettet = opprettet,
                        status = status,
                        beregning = meldekortbehandling.beregning,
                        saksbehandler = meldekortbehandling.saksbehandler!!,
                        beslutter = meldekortbehandling.beslutter!!,
                        brukerNavkontor = meldekortbehandling.navkontor,
                        rammevedtak = meldekortbehandling.rammevedtak!!,
                        automatiskBehandlet = meldekortbehandling is MeldekortBehandletAutomatisk,
                    )
                }

                BeregningKildeDb.BEHANDLING -> {
                    val behandlingId = BehandlingId.fromString(string("behandling_id"))

                    val rammevedtak = RammevedtakPostgresRepo.hentForBehandlingId(
                        behandlingId,
                        session,
                    )

                    requireNotNull(rammevedtak) {
                        "Fant ingen rammevedtak for $behandlingId på utbetalingsvedtak $vedtakId"
                    }

                    val behandling = rammevedtak.behandling as Revurdering

                    Utbetalingsvedtak(
                        id = vedtakId,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtakId,
                        sendtTilUtbetaling = sendtTilUtbetaling,
                        journalpostId = journalpostId,
                        journalføringstidspunkt = journalføringstidspunkt,
                        opprettet = opprettet,
                        status = status,
                        saksbehandler = behandling.saksbehandler!!,
                        beslutter = behandling.beslutter!!,
                        beregning = behandling.beregning!!,
                        brukerNavkontor = behandling.navkontor!!,
                        rammevedtak = listOf(rammevedtak.id),
                        automatiskBehandlet = false,
                    )
                }
            }
        }
    }
}

private enum class BeregningKildeDb {
    MELDEKORT,
    BEHANDLING,
}
