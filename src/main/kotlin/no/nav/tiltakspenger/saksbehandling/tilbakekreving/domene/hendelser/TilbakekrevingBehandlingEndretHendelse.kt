package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndretHendelse(
    override val id: TilbakekrevinghendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime?,
    override val sakId: SakId?,
    override val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val tilbakeBehandlingId: String,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val forrigeBehandlingsstatus: TilbakekrevingBehandlingsstatus?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val url: String,
    val fullstendigPeriode: Periode,
) : Tilbakekrevingshendelse {
    override val hendelsestype = TilbakekrevinghendelseType.BehandlingEndret

    private val logger = KotlinLogging.logger {}

    fun oppdaterBehandlingHvisEndret(behandling: TilbakekrevingBehandling): TilbakekrevingBehandling? {
        require(behandling.tilbakeBehandlingId == tilbakeBehandlingId) {
            "Forsøkte å oppdatere tilbakekreving-behandling ${behandling.tilbakeBehandlingId} med hendelse for behandling $tilbakeBehandlingId"
        }

        if (behandling.sistEndret >= this.opprettet) {
            logger.info { "BehandlingEndret hendelse $id er utdatert eller allerede behandlet - hopper over oppdatering" }
            return null
        }

        /*
         *  Tilbakeløsningen sender daglige oppdateringer for hver åpne behandling, selv om det ikke er noen faktiske endringer
         *  Vi ønsker ikke å oppdatere behandlingen vår når det ikke er noen endringer
         * */
        if (!harEndringer(behandling)) {
            logger.info { "BehandlingEndret hendelse $id har ingen endringer - hopper over oppdatering" }
            return null
        }

        return behandling.copy(
            status = behandlingsstatus,
            kravgrunnlagTotalPeriode = fullstendigPeriode,
            url = url,
            varselSendt = varselSendt,
            totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
            sistEndret = opprettet,
        )
    }

    private fun harEndringer(behandling: TilbakekrevingBehandling): Boolean {
        return behandling.status != behandlingsstatus ||
            behandling.kravgrunnlagTotalPeriode != fullstendigPeriode ||
            behandling.url !== url ||
            behandling.varselSendt != varselSendt ||
            behandling.totaltFeilutbetaltBeløp != totaltFeilutbetaltBeløp
    }
}
