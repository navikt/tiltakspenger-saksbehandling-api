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
                        varsel_sendt
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
                        :varsel_sendt
                    )
                    ON CONFLICT (id) DO UPDATE SET
                        status = :status,
                        url = :url,
                        kravgrunnlag_periode = :kravgrunnlag_periode::periode,
                        totalt_feilutbetalt_beløp = :totalt_feilutbetalt_belop,
                        varsel_sendt = :varsel_sendt,
                        sist_endret = :sist_endret
                    """.trimIndent(),
                    "id" to tilbakekrevingBehandling.id.toString(),
                    "sak_id" to tilbakekrevingBehandling.sakId.toString(),
                    "utbetaling_id" to tilbakekrevingBehandling.utbetalingId.toString(),
                    "tilbake_behandling_id" to tilbakekrevingBehandling.tilbakeBehandlingId,
                    "opprettet" to tilbakekrevingBehandling.opprettet,
                    "sist_endret" to tilbakekrevingBehandling.sistEndret,
                    "status" to tilbakekrevingBehandling.status.name,
                    "url" to tilbakekrevingBehandling.url,
                    "kravgrunnlag_periode" to tilbakekrevingBehandling.kravgrunnlagTotalPeriode.tilDbPeriode(),
                    "totalt_feilutbetalt_belop" to tilbakekrevingBehandling.totaltFeilutbetaltBeløp,
                    "varsel_sendt" to tilbakekrevingBehandling.varselSendt,
                ).asUpdate,
            )
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
                status = TilbakekrevingBehandlingsstatus.valueOf(string("status")),
                url = string("url"),
                kravgrunnlagTotalPeriode = periode("kravgrunnlag_periode"),
                totaltFeilutbetaltBeløp = bigDecimal("totalt_feilutbetalt_beløp"),
                varselSendt = localDateOrNull("varsel_sendt"),
            )
        }
    }
}
