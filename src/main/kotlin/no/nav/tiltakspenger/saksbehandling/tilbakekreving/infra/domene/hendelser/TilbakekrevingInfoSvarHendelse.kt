package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDateTime

data class TilbakekrevingInfoSvarHendelse(
    override val id: TilbakekrevingshendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime?,
    override val sakId: SakId?,
    override val eksternFagsakId: String,
    val behandlendeEnhet: String,
) : Tilbakekrevingshendelse {
    override val hendelsestype = TilbakekrevingHendelsestype.InfoSvar
}
