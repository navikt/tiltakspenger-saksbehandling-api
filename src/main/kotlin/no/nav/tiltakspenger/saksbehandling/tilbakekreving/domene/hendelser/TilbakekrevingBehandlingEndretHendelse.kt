package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndretHendelse(
    override val id: TilbakekrevingshendelseId,
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
    override val hendelsestype = TilbakekrevingHendelsestype.BehandlingEndret

    fun harEndringer(behandling: TilbakekrevingBehandling): Boolean {
        require(behandling.tilbakeBehandlingId == tilbakeBehandlingId) {
            "Prøvde å sammenligne endret-hendelse for tilbake behandling $tilbakeBehandlingId med behandling ${behandling.tilbakeBehandlingId}"
        }

        return behandling.status != behandlingsstatus ||
            behandling.kravgrunnlagTotalPeriode != fullstendigPeriode ||
            behandling.url !== url ||
            behandling.varselSendt != varselSendt ||
            behandling.totaltFeilutbetaltBeløp != totaltFeilutbetaltBeløp
    }
}
