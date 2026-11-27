package no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka

import java.util.UUID

/**
 * Husker produserte identhendelser i stedet for å sende dem til Kafka.
 * Testene asserter på [produserteHendelser] for å verifisere hva som ville gått ut på topicet.
 */
class IdenthendelseFakeProducer : IdenthendelseProducer {

    private val produserte = mutableListOf<Pair<UUID, IdenthendelseDto>>()

    val produserteHendelser: List<Pair<UUID, IdenthendelseDto>> get() = produserte.toList()

    override fun produserIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto) {
        produserte.add(id to identhendelseDto)
    }
}
