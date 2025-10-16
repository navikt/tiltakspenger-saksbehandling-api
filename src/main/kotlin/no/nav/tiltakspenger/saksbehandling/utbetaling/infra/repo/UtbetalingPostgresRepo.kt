package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraBehandling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraMeldekort
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import java.time.LocalDateTime

class UtbetalingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingRepo {
    override fun lagre(
        utbetaling: VedtattUtbetaling,
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

    override fun hentForUtsjekk(limit: Int): List<VedtattUtbetaling> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select u.* from utbetaling_full u
                    left join utbetaling parent on parent.id = u.forrige_utbetaling_id
                      and parent.sak_id = u.sak_id
                    where u.sendt_til_utbetaling_tidspunkt is null
                      and (u.forrige_utbetaling_id is null or (parent.sendt_til_utbetaling_tidspunkt is not null and parent.status IN ('OK','OK_UTEN_UTBETALING')))
                    order by u.opprettet
                    limit :limit
                    """.trimIndent(),
                    "limit" to limit,
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
                    select * from utbetaling_full
                    where (status is null or status IN ('IKKE_PÅBEGYNT', 'SENDT_TIL_OPPDRAG')) and sendt_til_utbetaling_tidspunkt is not null
                        and (status_metadata->>'nesteForsøk')::timestamptz <= now()
                    order by (status_metadata->>'antall_forsøk')::int, opprettet
                    limit :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    UtbetalingDetSkalHentesStatusFor(
                        utbetalingId = UtbetalingId.fromString(row.string("id")),
                        saksnummer = Saksnummer(row.string("saksnummer")),
                        sakId = SakId.fromString(row.string("sak_id")),
                        opprettet = row.localDateTime("opprettet"),
                        sendtTilUtbetalingstidspunkt = row.localDateTime("sendt_til_utbetaling_tidspunkt"),
                        forsøkshistorikk = row.string("status_metadata").toForsøkshistorikk(),
                    )
                }.asList,
            )
        }
    }

    companion object {
        fun hent(id: UtbetalingId, session: Session): VedtattUtbetaling? {
            return session.run(
                sqlQuery(
                    """
                select 
                    *
                from utbetaling_full
                where id = :id 
                """,
                    "id" to id.toString(),
                ).map {
                    it.tilUtbetaling()
                }.asSingle,
            )
        }

        fun lagre(utbetaling: VedtattUtbetaling, session: Session) {
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
                        status,
                        status_metadata,
                        opprettet,
                        satstype
                    ) values(
                        :id,
                        :sak_id,
                        :rammevedtak_id,
                        :meldekortvedtak_id,
                        :forrige_utbetaling_id,
                        :sendt_til_utbetaling_tidspunkt,
                        :status,
                        to_jsonb(:status_metadata::jsonb),
                        :opprettet,
                        :satstype
                    )
                    """,
                    "id" to utbetaling.id.toString(),
                    "sak_id" to utbetaling.sakId.toString(),
                    "forrige_utbetaling_id" to utbetaling.forrigeUtbetalingId?.toString(),
                    "sendt_til_utbetaling_tidspunkt" to utbetaling.sendtTilUtbetaling?.toString(),
                    "status" to utbetaling.status?.toString(),
                    "status_metadata" to utbetaling.statusMetadata.toDbJson(),
                    "opprettet" to utbetaling.opprettet,
                    "satstype" to utbetaling.satstype.tilDb(),
                    when (utbetaling.beregningKilde) {
                        is BeregningKilde.BeregningKildeBehandling -> "rammevedtak_id" to utbetaling.vedtakId.toString()
                        is BeregningKilde.BeregningKildeMeldekort -> "meldekortvedtak_id" to utbetaling.vedtakId.toString()
                    },
                ).asUpdate,
            )
        }

        private fun Row.tilUtbetaling(): VedtattUtbetaling {
            val id = UtbetalingId.fromString(string("id"))

            val meldekortVedtakId = stringOrNull("meldekortvedtak_id")
            val rammevedtakId = stringOrNull("rammevedtak_id")

            require((meldekortVedtakId != null).xor(rammevedtakId != null)) {
                "VedtakId for meldekortvedtak ELLER rammevedtak må være satt - Utbetalingen $id hadde $meldekortVedtakId / $rammevedtakId"
            }

            val beregningJson = string("beregning")

            val vedtakIdOgBeregning: Pair<VedtakId, Beregning> = meldekortVedtakId?.let {
                val vedtakId = VedtakId.fromString(it)
                val meldekortId = MeldekortId.fromString(string("meldekort_id"))
                vedtakId to Beregning(beregningJson.tilMeldeperiodeBeregningerFraMeldekort(meldekortId))
            } ?: run {
                val vedtakId = VedtakId.fromString(rammevedtakId!!)
                val behandlingId = BehandlingId.fromString(string("behandling_id"))
                vedtakId to Beregning(beregningJson.tilMeldeperiodeBeregningerFraBehandling(behandlingId))
            }

            return VedtattUtbetaling(
                id = UtbetalingId.fromString(string("id")),
                vedtakId = vedtakIdOgBeregning.first,
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
                beregning = vedtakIdOgBeregning.second,
                forrigeUtbetalingId = stringOrNull("forrige_utbetaling_id")
                    ?.let { UtbetalingId.fromString(it) },
                statusMetadata = string("status_metadata").toForsøkshistorikk(),
                satstype = string("satstype").tilSatstype(),
                status = stringOrNull("status")?.toUtbetalingsstatus(),
                sendtTilUtbetaling = localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                skalUtbetaleHelgPåFredag = boolean("kan_sende_inn_helg_for_meldekort"),
            )
        }
    }
}
