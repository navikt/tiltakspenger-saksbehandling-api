package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingshendelseId
import java.time.Clock
import java.time.LocalDateTime

data class TilbakekrevingInfoBehovDTO(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val kravgrunnlagReferanse: String,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1
    override val hendelsestype = TilbakekrevingHendelsestypeDTO.fagsysteminfo_behov

    override fun tilNyHendelse(key: String, clock: Clock): TilbakekrevingInfoBehovHendelse {
        return TilbakekrevingInfoBehovHendelse(
            id = TilbakekrevingshendelseId.random(),
            opprettet = nå(clock),
            sakId = null,
            behandlet = null,
            svar = null,
            eksternFagsakId = eksternFagsakId,
            kravgrunnlagReferanse = kravgrunnlagReferanse,
        )
    }
}
