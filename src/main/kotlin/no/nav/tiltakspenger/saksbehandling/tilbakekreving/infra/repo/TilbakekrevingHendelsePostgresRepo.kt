package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingUkjentHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import java.time.Clock

class TilbakekrevingHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : TilbakekrevingHendelseRepo {

    /**
     * Lagrer en ny tilbakekrevingshendelse
     *
     * Hendelser fra tilbakeløsningen har ingen unik id, men opprettet-tidspunktet for hendelsen skal normalt være unikt
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
                        ekstern_behandling_id,
                        behandling,
                        key,
                        value
                    ) VALUES (
                        :id,
                        :opprettet,
                        :hendelse_type,
                        :ekstern_fagsak_id,
                        :kravgrunnlag_referanse,
                        :ekstern_behandling_id,
                        to_jsonb(:behandling::jsonb),
                        :key,
                        to_jsonb(:value::jsonb)
                    )
                    ON CONFLICT (ekstern_fagsak_id, hendelse_type, opprettet) DO NOTHING
                    """.trimIndent(),
                    "id" to hendelse.id.toString(),
                    "opprettet" to hendelse.opprettet,
                    "ekstern_fagsak_id" to hendelse.eksternFagsakId,
                    "key" to key,
                    "value" to value,
                    *when (hendelse) {
                        is TilbakekrevingInfoBehovHendelse -> arrayOf(
                            "hendelse_type" to HendelsetypeDb.InfoBehov.toString(),
                            "kravgrunnlag_referanse" to hendelse.kravgrunnlagReferanse,
                        )

                        is TilbakekrevingBehandlingEndretHendelse -> arrayOf(
                            "hendelse_type" to HendelsetypeDb.BehandlingEndret.toString(),
                            "ekstern_behandling_id" to hendelse.eksternBehandlingId,
                            "behandling" to hendelse.tilDbBehandlingJson(),
                        )

                        is TilbakekrevingUkjentHendelse -> arrayOf(
                            "hendelse_type" to HendelsetypeDb.Ukjent.toString(),
                        )
                    },
                ).asUpdate,
            )
            rowsAffected > 0
        }
    }

    override fun hentUbehandledeHendelser(): List<Tilbakekrevingshendelse> =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_hendelse
                    WHERE behandlet IS NULL
                    ORDER BY opprettet
                    """.trimIndent(),
                ).map { row -> row.tilTilbakekrevingshendelse() }.asList,
            )
        }

    override fun markerInfoBehovSomBehandlet(
        hendelseId: TilbakekrevinghendelseId,
        sakId: SakId,
        svarJson: String,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet,
                        sak_id = :sak_id,
                        svar = to_jsonb(:svar::jsonb)
                    WHERE id = :id AND hendelse_type = 'InfoBehov'
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "sak_id" to sakId.toString(),
                    "svar" to svarJson,
                ).asUpdate,
            )
        }
    }

    override fun markerEndringSomBehandlet(
        hendelseId: TilbakekrevinghendelseId,
        sakId: SakId,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet,
                        sak_id = :sak_id
                    WHERE id = :id AND hendelse_type = 'BehandlingEndret'
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "sak_id" to sakId.toString(),
                ).asUpdate,
            )
        }
    }

    override fun markerSomBehandletMedFeil(
        hendelseId: TilbakekrevinghendelseId,
        sakId: SakId?,
        feil: TilbakekrevinghendelseFeil,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet,
                        behandlet_feil = :behandlet_feil,
                        sak_id = :sak_id
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "behandlet_feil" to feil.tilDb(),
                    "sak_id" to sakId?.toString(),
                ).asUpdate,
            )
        }
    }

    // Benyttes kun for tester, da vi normalt ikke trenger å hente en spesifikk hendelse
    override fun hentHendelse(hendelseId: TilbakekrevinghendelseId): Tilbakekrevingshendelse? =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT *
                    FROM tilbakekreving_hendelse
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                ).map { row -> row.tilTilbakekrevingshendelse() }.asSingle,
            )
        }

    override fun oppdaterUkjent(
        oppdatertHendelse: Tilbakekrevingshendelse,
        sessionContext: SessionContext?,
    ) {
        require(oppdatertHendelse !is TilbakekrevingUkjentHendelse) {
            "Kan ikke oppdatere ukjent-rad til ukjent type"
        }
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        opprettet = :opprettet,
                        hendelse_type = :hendelse_type,
                        ekstern_fagsak_id = :ekstern_fagsak_id,
                        kravgrunnlag_referanse = :kravgrunnlag_referanse,
                        ekstern_behandling_id = :ekstern_behandling_id,
                        behandling = to_jsonb(:behandling::jsonb)
                    WHERE id = :id AND hendelse_type = 'Ukjent'
                    """.trimIndent(),
                    "id" to oppdatertHendelse.id.toString(),
                    "opprettet" to oppdatertHendelse.opprettet,
                    "ekstern_fagsak_id" to oppdatertHendelse.eksternFagsakId,
                    *when (oppdatertHendelse) {
                        is TilbakekrevingInfoBehovHendelse -> arrayOf(
                            "hendelse_type" to HendelsetypeDb.InfoBehov.toString(),
                            "kravgrunnlag_referanse" to oppdatertHendelse.kravgrunnlagReferanse,
                            "ekstern_behandling_id" to null,
                            "behandling" to null,
                        )

                        is TilbakekrevingBehandlingEndretHendelse -> arrayOf(
                            "hendelse_type" to HendelsetypeDb.BehandlingEndret.toString(),
                            "kravgrunnlag_referanse" to null,
                            "ekstern_behandling_id" to oppdatertHendelse.eksternBehandlingId,
                            "behandling" to oppdatertHendelse.tilDbBehandlingJson(),
                        )

                        is TilbakekrevingUkjentHendelse -> error("Kan ikke oppdatere ukjent til ukjent")
                    },
                ).asUpdate,
            )
        }
    }
}

private enum class HendelsetypeDb {
    InfoBehov,
    BehandlingEndret,
    Ukjent,
}

private fun Row.tilTilbakekrevingshendelse(): Tilbakekrevingshendelse {
    val hendelsestype = HendelsetypeDb.valueOf(string("hendelse_type"))
    val id = TilbakekrevinghendelseId.fromString(string("id"))
    val opprettet = localDateTime("opprettet")
    val eksternFagsakId = stringOrNull("ekstern_fagsak_id")
    val sakId = stringOrNull("sak_id")?.let { SakId.fromString(it) }
    val behandlet = localDateTimeOrNull("behandlet")
    val feil = stringOrNull("behandlet_feil")?.let { TilbakekrevinghendelseFeilDb.valueOf(it).tilDomene() }

    return when (hendelsestype) {
        HendelsetypeDb.InfoBehov -> TilbakekrevingInfoBehovHendelse(
            id = id,
            opprettet = opprettet,
            behandlet = behandlet,
            sakId = sakId,
            svar = stringOrNull("svar")?.let { deserialize(it) },
            eksternFagsakId = eksternFagsakId!!,
            kravgrunnlagReferanse = string("kravgrunnlag_referanse"),
            feil = feil,
        )

        HendelsetypeDb.BehandlingEndret -> {
            val behandling = deserialize<TilbakekrevingHendelseBehandlingDb>(string("behandling"))
            TilbakekrevingBehandlingEndretHendelse(
                id = id,
                opprettet = opprettet,
                behandlet = behandlet,
                sakId = sakId,
                eksternFagsakId = eksternFagsakId!!,
                eksternBehandlingId = string("ekstern_behandling_id"),
                tilbakeBehandlingId = behandling.behandlingId,
                sakOpprettet = behandling.sakOpprettet,
                varselSendt = behandling.varselSendt,
                behandlingsstatus = behandling.behandlingsstatus.tilDomene(),
                forrigeBehandlingsstatus = behandling.forrigeBehandlingsstatus?.tilDomene(),
                totaltFeilutbetaltBeløp = behandling.totaltFeilutbetaltBeløp,
                url = behandling.saksbehandlingURL,
                fullstendigPeriode = behandling.fullstendigPeriode.toDomain(),
                feil = feil,
                venter = behandling.venter?.tilDomene(),
            )
        }

        HendelsetypeDb.Ukjent -> TilbakekrevingUkjentHendelse(
            id = id,
            opprettet = opprettet,
            behandlet = behandlet,
            feil = feil,
            value = string("value"),
        )
    }
}
