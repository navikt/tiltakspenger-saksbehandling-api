package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class TilbakekrevingHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : TilbakekrevingHendelseRepo {

    /**
     * Lagrer en ny tilbakekrevingshendelse
     *
     * Lagrer ikke duplikate info_behov-hendelser for samme kravgrunnlag_referanse
     * Team tilbake har retry på denne hvis vi ikke svarer i løpet av 3 timer, vi ønsker ikke å svare for samme kravgrunnlag flere ganger
     */
    override fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        sakId: SakId?,
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
                        sak_id,
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
                        :sak_id,
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
                    "sak_id" to sakId?.toString(),
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
                            "behandling" to hendelse.tilDbBehandling(),
                        )
                    },
                ).asUpdate,
            )
            rowsAffected > 0
        }
    }

    override fun hentUbehandledeHendelser(): List<Tilbakekrevingshendelse> {
        return sessionFactory.withSession { session ->
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
    }

    override fun markerInfoBehovSomBehandlet(
        hendelseId: TilbakekrevinghendelseId,
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
                        svar = to_jsonb(:svar::jsonb)
                    WHERE id = :id AND hendelse_type = 'InfoBehov'
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "svar" to svarJson,
                ).asUpdate,
            )
        }
    }

    override fun markerEndringSomBehandlet(
        hendelseId: TilbakekrevinghendelseId,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE tilbakekreving_hendelse
                    SET
                        behandlet = :behandlet
                    WHERE id = :id AND hendelse_type = 'BehandlingEndret'
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                ).asUpdate,
            )
        }
    }

    override fun markerSomBehandletMedFeil(
        hendelseId: TilbakekrevinghendelseId,
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
                        behandlet_feil = :behandlet_feil
                    WHERE id = :id
                    """.trimIndent(),
                    "id" to hendelseId.toString(),
                    "behandlet" to nå(clock),
                    "behandlet_feil" to feil.tilDb(),
                ).asUpdate,
            )
        }
    }

    // Benyttes kun for tester, da vi normalt ikke trenger å hente en spesifikk hendelse
    override fun hentHendelse(hendelseId: TilbakekrevinghendelseId): Tilbakekrevingshendelse? {
        return sessionFactory.withSession { session ->
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
    }

    private fun Row.tilTilbakekrevingshendelse(): Tilbakekrevingshendelse {
        val hendelsestype = HendelsetypeDb.valueOf(string("hendelse_type"))
        val id = TilbakekrevinghendelseId.fromString(string("id"))
        val opprettet = localDateTime("opprettet")
        val eksternFagsakId = string("ekstern_fagsak_id")

        val sakId = stringOrNull("sak_id")?.let { SakId.fromString(it) }
        val behandlet = localDateTimeOrNull("behandlet")
        val feil = stringOrNull("behandlet_feil")?.let { TilbakekrevinghendelseFeilDb.valueOf(it).tilDomene() }

        return when (hendelsestype) {
            HendelsetypeDb.InfoBehov -> {
                TilbakekrevingInfoBehovHendelse(
                    id = id,
                    opprettet = opprettet,
                    behandlet = behandlet,
                    sakId = sakId,
                    svar = stringOrNull("svar")?.let { deserialize(it) },
                    eksternFagsakId = eksternFagsakId,
                    kravgrunnlagReferanse = string("kravgrunnlag_referanse"),
                    feil = feil,
                )
            }

            HendelsetypeDb.BehandlingEndret -> {
                val behandling = deserialize<TilbakekrevingBehandlingDb>(string("behandling"))

                TilbakekrevingBehandlingEndretHendelse(
                    id = id,
                    opprettet = opprettet,
                    behandlet = behandlet,
                    sakId = sakId,
                    eksternFagsakId = eksternFagsakId,
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
                )
            }
        }
    }
}

private enum class HendelsetypeDb {
    InfoBehov,
    BehandlingEndret,
}

private data class TilbakekrevingBehandlingDb(
    val behandlingId: String,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: BehandlingsstatusDb,
    val forrigeBehandlingsstatus: BehandlingsstatusDb?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: PeriodeDbJson,
) {

    enum class BehandlingsstatusDb {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
        ;

        fun tilDomene(): TilbakekrevingBehandlingsstatus {
            return when (this) {
                OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
                TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
                TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
                AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
            }
        }
    }
}

private fun TilbakekrevingBehandlingEndretHendelse.tilDbBehandling(): String {
    return TilbakekrevingBehandlingDb(
        behandlingId = tilbakeBehandlingId,
        sakOpprettet = sakOpprettet,
        varselSendt = varselSendt,
        behandlingsstatus = behandlingsstatus.tilDb(),
        forrigeBehandlingsstatus = forrigeBehandlingsstatus?.tilDb(),
        totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
        saksbehandlingURL = url,
        fullstendigPeriode = fullstendigPeriode.toDbJson(),
    ).let { serialize(it) }
}

private fun TilbakekrevingBehandlingsstatus.tilDb(): TilbakekrevingBehandlingDb.BehandlingsstatusDb {
    return when (this) {
        TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingDb.BehandlingsstatusDb.OPPRETTET
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> TilbakekrevingBehandlingDb.BehandlingsstatusDb.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> TilbakekrevingBehandlingDb.BehandlingsstatusDb.TIL_GODKJENNING
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingDb.BehandlingsstatusDb.AVSLUTTET
    }
}

enum class TilbakekrevinghendelseFeilDb {
    FantIkkeSak,
    FantIkkeBehandling,
    FantIkkeUtbetaling,
    ;

    fun tilDomene(): TilbakekrevinghendelseFeil {
        return when (this) {
            FantIkkeSak -> TilbakekrevinghendelseFeil.FantIkkeSak
            FantIkkeBehandling -> TilbakekrevinghendelseFeil.FantIkkeBehandling
            FantIkkeUtbetaling -> TilbakekrevinghendelseFeil.FantIkkeUtbetaling
        }
    }
}

private fun TilbakekrevinghendelseFeil.tilDb(): String {
    return when (this) {
        TilbakekrevinghendelseFeil.FantIkkeSak -> TilbakekrevinghendelseFeilDb.FantIkkeSak
        TilbakekrevinghendelseFeil.FantIkkeBehandling -> TilbakekrevinghendelseFeilDb.FantIkkeBehandling
        TilbakekrevinghendelseFeil.FantIkkeUtbetaling -> TilbakekrevinghendelseFeilDb.FantIkkeUtbetaling
    }.toString()
}
