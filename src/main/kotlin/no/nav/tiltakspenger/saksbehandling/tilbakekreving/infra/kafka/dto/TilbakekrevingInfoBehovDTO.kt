package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import java.time.LocalDateTime

data class TilbakekrevingInfoBehovDTO(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val kravgrunnlagReferanse: String,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1
    override val hendelsestype = TilbakekrevingHendelsestypeDTO.fagsysteminfo_behov

    override fun tilHendelseForLagring(key: String): TilbakekrevingInfoBehovHendelse {
        return TilbakekrevingInfoBehovHendelse(
            id = TilbakekrevinghendelseId.random(),
            opprettet = hendelseOpprettet,
            eksternFagsakId = eksternFagsakId,
            kravgrunnlagReferanse = kravgrunnlagReferanse,
            sakId = null,
            behandlet = null,
            svar = null,
            feil = null,
        )
    }
}
