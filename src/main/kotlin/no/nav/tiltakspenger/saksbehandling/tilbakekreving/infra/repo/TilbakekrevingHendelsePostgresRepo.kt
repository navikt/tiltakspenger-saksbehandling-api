package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingHendelsestype
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoSvarHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId
import java.time.Clock

class TilbakekrevingHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : TilbakekrevingHendelseRepo {

    /**
     * Lagrer en ny tilbakekrevingshendelse. Returnerer true hvis hendelsen ble lagret, false hvis den allerede eksisterer (duplikat).
     */
    override fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        key: String,
        value: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            val rowsAffected = session.run(
                sqlQuery(
                    """
                    INSERT INTO tilbakekreving_hendelse (
                        id,
                        opprettet,
                        hendelse_type,
                        ekstern_fagsak_id,
                        kravgrunnlag_referanse,
                        url,
                        behandlingsstatus,
                        key,
                        value
                    ) VALUES (
                        :id,
                        :opprettet,
                        :hendelse_type,
                        :ekstern_fagsak_id,
                        :kravgrunnlag_referanse,
                        :url,
                        :behandlingsstatus,
                        :key,
                        to_jsonb(:value::jsonb)
                    )
                    ON CONFLICT (kravgrunnlag_referanse) DO NOTHING
                    """.trimIndent(),
                    "id" to hendelse.id.toString(),
                    "opprettet" to hendelse.opprettet,
                    "hendelse_type" to hendelse.hendelsestype.toString(),
                    "ekstern_fagsak_id" to hendelse.eksternFagsakId,
                    "key" to key,
                    "value" to value,
                    *when (hendelse) {
                        is TilbakekrevingInfoBehovHendelse -> arrayOf(
                            "kravgrunnlag_referanse" to hendelse.kravgrunnlagReferanse,
                        )

                        is TilbakekrevingBehandlingEndretHendelse -> arrayOf(
                            "url" to hendelse.url,
                            "behandlingsstatus" to hendelse.behandlingsstatus,
                        )

                        is TilbakekrevingInfoSvarHendelse -> throw IllegalArgumentException(
                            "Skal ikke lagre InfoSvarHendelse som ny hendelse, skal kun oppdatere eksisterende InfoBehovHendelse med svar",
                        )
                    },
                ).asUpdate,
            )
            rowsAffected > 0
        }
    }

    override fun oppdaterBehandletInfoBehovMedSvar(hendelseId: TilbakekrevingshendelseId, svarJson: String) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet,
                        svar = to_jsonb(:svar::jsonb)
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "svar" to svarJson,
                ).asUpdate,
            )
        }
    }

    override fun oppdaterBehandletInfoBehovFeil(hendelseId: TilbakekrevingshendelseId, feil: String) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet,
                        behandlet_feil = :behandlet_feil
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "behandlet_feil" to feil,
                ).asUpdate,
            )
        }
    }

    override fun hentUbehandledeInfoBehov(): List<TilbakekrevingInfoBehovHendelse> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_hendelse
                    WHERE hendelse_type = :hendelse_type
                    AND behandlet IS NULL
                    """.trimIndent(),
                    "hendelse_type" to TilbakekrevingHendelsestype.InfoBehov.toString(),
                ).map { row -> row.fromRow() as TilbakekrevingInfoBehovHendelse }.asList,
            )
        }
    }

    private fun Row.fromRow(): Tilbakekrevingshendelse {
        val hendelsestype = TilbakekrevingHendelsestype.valueOf(string("hendelse_type"))
        val id = TilbakekrevingshendelseId.fromString(string("id"))
        val opprettet = localDateTime("opprettet")
        val eksternFagsakId = string("ekstern_fagsak_id")

        val sakId = stringOrNull("sak_id")?.let { SakId.fromString(it) }
        val behandlet = localDateTimeOrNull("behandlet")

        return when (hendelsestype) {
            TilbakekrevingHendelsestype.InfoBehov -> {
                TilbakekrevingInfoBehovHendelse(
                    id = id,
                    opprettet = opprettet,
                    behandlet = behandlet,
                    sakId = sakId,
                    svar = stringOrNull("svar")?.let { deserialize(it) },
                    eksternFagsakId = eksternFagsakId,
                    kravgrunnlagReferanse = string("kravgrunnlag_referanse"),
                    feil = stringOrNull("behandlet_feil"),
                )
            }

            TilbakekrevingHendelsestype.InfoSvar,
            TilbakekrevingHendelsestype.BehandlingEndret,
            -> TODO()
        }
    }
}
