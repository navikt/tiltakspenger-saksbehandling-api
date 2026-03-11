package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import java.math.BigDecimal
import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndretHendelse(
    override val id: TilbakekrevingshendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime?,
    override val sakId: SakId?,
    override val eksternFagsakId: String,
    val tilbakekrevingBehandlingId: String,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: String,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val url: String,
    val fullstendigPeriode: Periode,
) : Tilbakekrevingshendelse {
    override val hendelsestype = TilbakekrevingHendelsestype.BehandlingEndret
}
