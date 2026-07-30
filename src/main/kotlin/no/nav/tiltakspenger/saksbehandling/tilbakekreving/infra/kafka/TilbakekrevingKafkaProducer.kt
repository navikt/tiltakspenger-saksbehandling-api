package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.Producer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO

class TilbakekrevingKafkaProducer(
    private val topic: String,
    private val kafkaProducer: Producer<String, String> = Producer(producerConfig = KafkaConfig.fraNaisEnv().producerConfig()),
) : TilbakekrevingProducer {

    override fun produserInfoSvar(behovHendelseId: TilbakekrevinghendelseId, infoSvar: TilbakekrevingInfoSvarDTO): String {
        val json = serialize(infoSvar)

        kafkaProducer.produce(topic, behovHendelseId.uuidPart(), json)

        return json
    }
}
