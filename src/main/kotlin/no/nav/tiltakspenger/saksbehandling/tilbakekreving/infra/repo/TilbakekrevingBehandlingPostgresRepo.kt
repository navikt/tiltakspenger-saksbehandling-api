package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId

class TilbakekrevingBehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : TilbakekrevingBehandlingRepo {

    override fun lagre(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    INSERT INTO tilbakekreving_behandling (
                        id,
                        sak_id,
                        utbetaling_id,
                        tilbake_behandling_id,
                        opprettet,
                        sist_endret,
                        status,
                        url,
                        kravgrunnlag_periode,
                        totalt_feilutbetalt_beløp,
                        varsel_sendt,
                        saksbehandler_ident,
                        beslutter_ident
                    ) VALUES (
                        :id,
                        :sak_id,
                        :utbetaling_id,
                        :tilbake_behandling_id,
                        :opprettet,
                        :sist_endret,
                        :status,
                        :url,
                        :kravgrunnlag_periode::periode,
                        :totalt_feilutbetalt_belop,
                        :varsel_sendt,
                        :saksbehandler_ident,
                        :beslutter_ident
                    )
                    ON CONFLICT (id) DO UPDATE SET
                        status = :status,
                        url = :url,
                        kravgrunnlag_periode = :kravgrunnlag_periode::periode,
                        totalt_feilutbetalt_beløp = :totalt_feilutbetalt_belop,
                        varsel_sendt = :varsel_sendt,
                        sist_endret = :sist_endret,
                        saksbehandler_ident = :saksbehandler_ident,
                        beslutter_ident = :beslutter_ident
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "sak_id" to tilbakekrevingBehandling.sakId.toString(),
                    "utbetaling_id" to tilbakekrevingBehandling.utbetalingId.toString(),
                    "tilbake_behandling_id" to tilbakekrevingBehandling.tilbakeBehandlingId,
                    "opprettet" to tilbakekrevingBehandling.opprettet,
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                    "status" to tilbakekrevingBehandling.status.tilDb(),
                    "url" to tilbakekrevingBehandling.url,
                    "kravgrunnlag_periode" to tilbakekrevingBehandling.kravgrunnlagTotalPeriode.tilDbPeriode(),
                    "totalt_feilutbetalt_belop" to tilbakekrevingBehandling.totaltFeilutbetaltBeløp,
                    "varsel_sendt" to tilbakekrevingBehandling.varselSendt,
                    "saksbehandler_ident" to tilbakekrevingBehandling.saksbehandler,
                    "beslutter_ident" to tilbakekrevingBehandling.beslutter,
                ).asUpdate,
            )
        }
    }

    override fun taBehandlingSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        saksbehandler_ident = :saksbehandler_ident,
                        beslutter_ident = CASE WHEN beslutter_ident = :saksbehandler_ident THEN null ELSE beslutter_ident END,
                        status = :status,
                        sist_endret = :sist_endret
                    WHERE id = :id AND saksbehandler_ident IS NULL AND status = 'TIL_BEHANDLING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "saksbehandler_ident" to tilbakekrevingBehandling.saksbehandler,
                    "status" to tilbakekrevingBehandling.status.tilDb(),
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun taBehandlingBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        beslutter_ident = :beslutter_ident,
                        status = :status,
                        sist_endret = :sist_endret
                    WHERE id = :id AND beslutter_ident IS NULL AND status = 'TIL_GODKJENNING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "beslutter_ident" to tilbakekrevingBehandling.beslutter,
                    "status" to tilbakekrevingBehandling.status.tilDb(),
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun overtaSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        saksbehandler_ident = :ny_saksbehandler,
                        beslutter_ident = CASE WHEN beslutter_ident = :ny_saksbehandler THEN null ELSE beslutter_ident END,
                        sist_endret = :sist_endret
                    WHERE id = :id AND saksbehandler_ident = :naverende_saksbehandler AND status = 'TIL_BEHANDLING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "ny_saksbehandler" to tilbakekrevingBehandling.saksbehandler,
                    "naverende_saksbehandler" to nåværendeSaksbehandler,
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun overtaBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        beslutter_ident = :ny_beslutter,
                        sist_endret = :sist_endret
                    WHERE id = :id AND beslutter_ident = :naverende_beslutter AND status = 'TIL_GODKJENNING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "ny_beslutter" to tilbakekrevingBehandling.beslutter,
                    "naverende_beslutter" to nåværendeBeslutter,
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        saksbehandler_ident = null,
                        status = :status,
                        sist_endret = :sist_endret
                    WHERE id = :id AND saksbehandler_ident = :naverende_saksbehandler AND status = 'TIL_BEHANDLING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "naverende_saksbehandler" to nåværendeSaksbehandler,
                    "status" to tilbakekrevingBehandling.status.tilDb(),
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_behandling SET
                        beslutter_ident = null,
                        status = :status,
                        sist_endret = :sist_endret
                    WHERE id = :id AND beslutter_ident = :naverende_beslutter AND status = 'TIL_GODKJENNING'
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "naverende_beslutter" to nåværendeBeslutter,
                    "status" to tilbakekrevingBehandling.status.tilDb(),
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun hent(id: TilbakekrevingId, sessionContext: SessionContext?): TilbakekrevingBehandling? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_behandling
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to id.toString(),
                ).map { row -> row.tilTilbakekrevingBehandling() }.asSingle,
            )
        }
    }

    override fun hentForTilbakeBehandlingId(id: String, sessionContext: SessionContext?): TilbakekrevingBehandling? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_behandling
                    WHERE tilbake_behandling_id = :id
                    """.trimIndent(),
                    "id" to id,
                ).map { row -> row.tilTilbakekrevingBehandling() }.asSingle,
            )
        }
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): List<TilbakekrevingBehandling> {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    override fun hentForUtbetalingId(
        utbetalingId: UtbetalingId,
        sessionContext: SessionContext?,
    ): List<TilbakekrevingBehandling> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_behandling
                    WHERE utbetaling_id = :utbetaling_id
                    """.trimIndent(),
                    "utbetaling_id" to utbetalingId.toString(),
                ).map { row -> row.tilTilbakekrevingBehandling() }.asList,
            )
        }
    }

    companion object {
        fun hentForSakId(sakId: SakId, session: Session): List<TilbakekrevingBehandling> {
            return session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_behandling
                    WHERE sak_id = :sak_id
                    """.trimIndent(),
                    "sak_id" to sakId.toString(),
                ).map { row -> row.tilTilbakekrevingBehandling() }.asList,
            )
        }

        private fun Row.tilTilbakekrevingBehandling(): TilbakekrevingBehandling {
            return TilbakekrevingBehandling(
                id = TilbakekrevingId.fromString(string("id")),
                sakId = SakId.fromString(string("sak_id")),
                utbetalingId = UtbetalingId.fromString(string("utbetaling_id")),
                tilbakeBehandlingId = string("tilbake_behandling_id"),
                opprettet = localDateTime("opprettet"),
                sistEndret = localDateTime("sist_endret"),
                status = TilbakekrevingBehandlingsstatusDb.valueOf(string("status")).tilDomene(),
                url = string("url"),
                kravgrunnlagTotalPeriode = periode("kravgrunnlag_periode"),
                totaltFeilutbetaltBeløp = bigDecimal("totalt_feilutbetalt_beløp"),
                varselSendt = localDateOrNull("varsel_sendt"),
                saksbehandler = stringOrNull("saksbehandler_ident"),
                beslutter = stringOrNull("beslutter_ident"),
            )
        }
    }
}

private enum class TilbakekrevingBehandlingsstatusDb {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_GODKJENNING,
    AVSLUTTET,
    ;

    fun tilDomene() = when (this) {
        OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
        TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
        AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
    }
}

private fun TilbakekrevingBehandlingsstatus.tilDb(): String {
    return when (this) {
        TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingsstatusDb.OPPRETTET
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> TilbakekrevingBehandlingsstatusDb.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> TilbakekrevingBehandlingsstatusDb.TIL_GODKJENNING
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingsstatusDb.AVSLUTTET
    }.name
}
