package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO

interface TilbakekrevingProducer {

    fun produserInfoSvar(behovHendelseId: TilbakekrevinghendelseId, infoSvar: TilbakekrevingInfoSvarDTO): String
}
