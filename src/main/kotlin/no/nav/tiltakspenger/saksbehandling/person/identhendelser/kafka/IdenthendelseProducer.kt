package no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka

import java.util.UUID

/**
 * Produserer identhendelser til vårt interne identhendelse-topic, som konsumeres av de andre tiltakspenger-appene.
 * Grensesnittet finnes for at testene skal kunne bytte ut Kafka med en fake, jf. [no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingProducer].
 */
interface IdenthendelseProducer {

    fun produserIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto)
}
