package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO

interface TilbakekrevingProducer {

    fun produserInfoSvar(behovHendelseId: TilbakekrevingshendelseId, infoSvar: TilbakekrevingInfoSvarDTO): String
}
