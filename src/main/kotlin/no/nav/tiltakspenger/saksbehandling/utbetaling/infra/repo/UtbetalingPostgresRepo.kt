package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
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
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String? {
        TODO("Not yet implemented")
    }

    override fun hentForUtsjekk(limit: Int): List<Utbetaling> {
        TODO("Not yet implemented")
    }

    override fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        TODO("Not yet implemented")
    }

    companion object {
        fun hent(id: UtbetalingId, session: Session) {
            session.run(
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
                            forrige_utbetaling_vedtak_id,
                            sendt_til_utbetaling_tidspunkt,
                            status
                        ) values(
                            :id,
                            :sak_id,
                            :rammevedtak_id,
                            :meldekortvedtak_id,
                            :forrige_utbetaling_vedtak_id,
                            :sendt_til_utbetaling_tidspunkt,
                            :status
                        )
                    """,
                    "id" to utbetaling.id.toString(),
                    "sak_id" to utbetaling.sakId.toString(),
                    "forrige_utbetaling_vedtak_id" to utbetaling.forrigeUtbetalingVedtakId?.toString(),
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
            return Utbetaling(
                id = UtbetalingId.fromString(string("id")),
                vedtakId = VedtakId.fromString(string("vedtak_id")),
                sakId = SakId.fromString(string("sak_id")),
                saksnummer = Saksnummer(string("saksnummer")),
                fnr = Fnr.fromString(string("fnr")),
                brukerNavkontor = TODO(),
                opprettet = TODO(),
                saksbehandler = TODO(),
                beslutter = TODO(),
                beregning = TODO(),
                forrigeUtbetalingVedtakId = TODO(),
                sendtTilUtbetaling = TODO(),
                status = TODO(),
            )
        }
    }
}
