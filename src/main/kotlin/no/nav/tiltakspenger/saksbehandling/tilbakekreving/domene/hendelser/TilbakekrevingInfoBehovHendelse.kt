package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import java.time.LocalDateTime

data class TilbakekrevingInfoBehovHendelse(
    override val id: TilbakekrevingshendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime?,
    override val sakId: SakId?,
    override val eksternFagsakId: String,
    val kravgrunnlagReferanse: String,
    val svar: TilbakekrevingInfoSvarDTO?,
    val feil: String?,
) : Tilbakekrevingshendelse {
    override val hendelsestype: TilbakekrevingHendelsestype = TilbakekrevingHendelsestype.InfoBehov
}
