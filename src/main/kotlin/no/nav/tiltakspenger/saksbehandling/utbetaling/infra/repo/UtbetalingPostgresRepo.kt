package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningFraMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningFraRammebehandling
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ProdusertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingResponse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingsreferanseTreff
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime

class UtbetalingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingRepo {
    override fun hent(utbetalingId: UtbetalingId, context: TransactionContext): VedtattUtbetaling? =
        sessionFactory.withSession(context) { session ->
            hent(utbetalingId, session)
        }

    override fun finnUtbetalingerForKvitteringsreferanser(
        referanser: Set<Utbetalingsreferanse>,
        context: TransactionContext,
    ): List<UtbetalingsreferanseTreff> {
        if (referanser.isEmpty()) return emptyList()
        val sorterteReferanser = referanser.sortedBy { it.verdi }
        val betingelser = sorterteReferanser.indices.joinToString(", ") { ":referanse$it" }
        val parametere = sorterteReferanser.mapIndexed { index, referanse ->
            "referanse$index" to (referanse.verdi as Any?)
        }.toTypedArray()
        return sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                    select right(u.id, 15) as referanse, u.id as utbetaling_id, u.sak_id
                    from utbetaling u
                    where right(u.id, 15) in ($betingelser)
                    order by right(u.id, 15), u.id
                    for update
                    """,
                    *parametere,
                ).map { row ->
                    UtbetalingsreferanseTreff(
                        referanse = Utbetalingsreferanse(row.string("referanse")),
                        kobling = Utbetalingskobling(
                            utbetalingId = UtbetalingId.fromString(row.string("utbetaling_id")),
                            sakId = SakId.fromString(row.string("sak_id")),
                        ),
                    )
                }.asList,
            )
        }
    }

    override fun antallFeiledeUtbetalingerPåVersjon3(): Long =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select count(*) as antall
                    from utbetaling u
                    join sak s on s.id = u.sak_id
                    where s.oppdragsversjon = 3
                      and u.status = 'FEILET_MOT_OPPDRAG'
                    """,
                ).map { it.long("antall") }.asList,
            ).single()
        }

    override fun antallUtbetalingerUtenOkPåVersjon3(eldreEnn: LocalDateTime): Long =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select count(*) as antall
                    from utbetaling u
                    join sak s on s.id = u.sak_id
                    where s.oppdragsversjon = 3
                      and u.opprettet <= :eldre_enn
                      and (
                        u.status is null
                        or u.status in ('IKKE_PÅBEGYNT', 'SENDT_TIL_OPPDRAG')
                      )
                    """,
                    "eldre_enn" to eldreEnn,
                ).map { it.long("antall") }.asList,
            ).single()
        }

    override fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        context: TransactionContext,
    ) {
        sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                    update utbetaling
                    set sendt_til_utbetaling_tidspunkt = :tidspunkt
                    where id = :id
                    """,
                    "id" to utbetalingId.toString(),
                    "tidspunkt" to tidspunkt,
                ).asUpdate,
            )
        }
    }

    override fun lagreUtbetalingsrespons(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: UtbetalingResponse,
        context: TransactionContext?,
    ) {
        sessionFactory.withSession(context) { session ->
            if (utbetalingsrespons is ProdusertUtbetaling) {
                val serialisertRequest = serialize(utbetalingsrespons.request)
                val antallOppdatert = session.run(
                    sqlQuery(
                        """
                        UPDATE utbetaling
                        SET utbetaling_metadata = jsonb_set(
                            COALESCE(utbetaling_metadata, '{}'::jsonb),
                            '{request}',
                            :request_json::jsonb,
                            true
                        )
                        WHERE id = :id
                          AND utbetaling_metadata->>'request' = :request
                        """,
                        "id" to utbetalingId.toString(),
                        "request" to utbetalingsrespons.request,
                        "request_json" to serialisertRequest,
                    ).asUpdate,
                )
                check(antallOppdatert == 1) {
                    "Fant ikke den lagrede V3-requesten for utbetaling $utbetalingId"
                }
            } else {
                session.run(
                    sqlQuery(
                        """
                        update utbetaling
                        set utbetaling_metadata = :metadata::jsonb
                        where id = :id
                        """,
                        "id" to utbetalingId.toString(),
                        "metadata" to utbetalingsrespons.toJson(),
                    ).asUpdate,
                )
            }
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

    override fun lagreUtbetalingJsonHvisMangler(
        utbetalingId: UtbetalingId,
        request: String,
        context: TransactionContext,
    ): String? = sessionFactory.withSession(context) { session ->
        val serialisertRequest = serialize(request)
        session.run(
            sqlQuery(
                """
                UPDATE utbetaling
                SET utbetaling_metadata = jsonb_set(
                    COALESCE(utbetaling_metadata, '{}'::jsonb),
                    '{request}',
                    :request_json::jsonb,
                    true
                )
                WHERE id = :id
                  AND utbetaling_metadata->>'request' IS NULL
                """,
                "id" to utbetalingId.toString(),
                "request_json" to serialisertRequest,
            ).asUpdate,
        )

        session.run(
            sqlQuery(
                """
                SELECT utbetaling_metadata->>'request' AS request
                FROM utbetaling
                WHERE id = :id
                """,
                "id" to utbetalingId.toString(),
            ).map { it.stringOrNull("request") }.asList,
        ).firstOrNull()
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
                    status_metadata = :status_metadata::jsonb
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
                    where (status is null or status IN ('IKKE_PÅBEGYNT', 'SENDT_TIL_OPPDRAG', 'FEILET_MOT_OPPDRAG'))
                        and sendt_til_utbetaling_tidspunkt is not null
                        and (status_metadata->>'nesteForsøk')::timestamptz <= now()
                        and oppdragsversjon = 2
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

    override fun hentOppdragsversjon(
        sakId: SakId,
        context: TransactionContext?,
    ): Oppdragsversjon = sessionFactory.withSession(context) { session ->
        session.run(
            sqlQuery(
                "select oppdragsversjon from sak where id = :id",
                "id" to sakId.toString(),
            ).map { it.int("oppdragsversjon").tilOppdragsversjon() }.asSingle,
        )
    } ?: throw IllegalStateException("Fant ikke sak $sakId")

    override fun lagreKvitteringsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        context: TransactionContext?,
    ): KvitteringsstatusLagring = sessionFactory.withSession(context) { session ->
        val oppdatert = session.run(
            sqlQuery(
                """
                update utbetaling u
                set status = :status
                from sak s
                where u.id = :id
                  and s.id = u.sak_id
                  and s.oppdragsversjon = 3
                  and not (
                    coalesce(u.status, '') in (
                      'OK',
                      'OK_UTEN_UTBETALING',
                      'FEILET_MOT_OPPDRAG',
                      'AVBRUTT'
                    )
                    and :status not in (
                      'OK',
                      'OK_UTEN_UTBETALING',
                      'FEILET_MOT_OPPDRAG',
                      'AVBRUTT'
                    )
                  )
                """,
                "id" to utbetalingId.toString(),
                "status" to status.toDbType(),
            ).asUpdate,
        )
        if (oppdatert > 0) {
            KvitteringsstatusLagring.Lagret
        } else {
            val terminal = session.run(
                sqlQuery(
                    """
                    select coalesce(
                        u.status in (
                            'OK',
                            'OK_UTEN_UTBETALING',
                            'FEILET_MOT_OPPDRAG',
                            'AVBRUTT'
                        ),
                        false
                    ) as terminal
                    from utbetaling u
                    join sak s on s.id = u.sak_id
                    where u.id = :id
                      and s.oppdragsversjon = 3
                    """,
                    "id" to utbetalingId.toString(),
                ).map { it.boolean("terminal") }.asSingle,
            )
            if (terminal == true) {
                KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal
            } else {
                KvitteringsstatusLagring.FantIkkeUtbetaling
            }
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

        /**
         * En ny utbetaling er hverken sendt eller kvittert.
         * `sendt_til_utbetaling_tidspunkt` eies av [markerSendtTilUtbetaling] og `status` av [oppdaterUtbetalingsstatus], og utelates derfor her.
         */
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
                        status_metadata,
                        opprettet,
                        satstype
                    ) values(
                        :id,
                        :sak_id,
                        :rammevedtak_id,
                        :meldekortvedtak_id,
                        :forrige_utbetaling_id,
                        :status_metadata::jsonb,
                        :opprettet,
                        :satstype
                    )
                    """,
                    "id" to utbetaling.id.toString(),
                    "sak_id" to utbetaling.sakId.toString(),
                    "forrige_utbetaling_id" to utbetaling.forrigeUtbetalingId?.toString(),
                    "status_metadata" to utbetaling.statusMetadata.toDbJson(),
                    "opprettet" to utbetaling.opprettet,
                    "satstype" to utbetaling.satstype.tilDb(),
                    when (utbetaling.beregningKilde) {
                        is BeregningKilde.BeregningKildeRammebehandling -> "rammevedtak_id" to utbetaling.vedtakId.toString()
                        is BeregningKilde.BeregningKildeMeldekort -> "meldekortvedtak_id" to utbetaling.vedtakId.toString()
                    },
                ).asUpdate,
            )
        }

        /**
         * `rammevedtakId!!` er trygt: check-constrainten `rammevedtak_eller_meldekortvedtak` garanterer at nøyaktig én av de to kolonnene er satt.
         * `UtbetalingPostgresRepoNegativTest` verifiserer at constrainten fortsatt finnes.
         */
        private fun Row.tilUtbetaling(): VedtattUtbetaling {
            val meldekortvedtakId = stringOrNull("meldekortvedtak_id")
            val rammevedtakId = stringOrNull("rammevedtak_id")

            val beregningJson = string("beregning")

            val vedtakIdOgBeregning: Pair<VedtakId, Beregning> = if (meldekortvedtakId == null) {
                val vedtakId = VedtakId.fromString(rammevedtakId!!)
                val behandlingId = RammebehandlingId.fromString(string("behandling_id"))
                vedtakId to beregningJson.tilBeregningFraRammebehandling(behandlingId)
            } else {
                val vedtakId = VedtakId.fromString(meldekortvedtakId)
                val meldekortId = MeldekortId.fromString(string("meldekort_id"))
                vedtakId to beregningJson.tilBeregningFraMeldekortbehandling(meldekortId)
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
            )
        }
    }
}
