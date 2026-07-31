package no.nav.tiltakspenger.saksbehandling.common

/**
 * Adressene til det lokale Kafka-oppsettet.
 *
 * De bor her, i testkoden, og ikke i `ApplicationContext`.
 * Prodkoden er NAIS-oppsettet og skal verken kjenne til lokale adresser eller ha miljø-if-er - se «Miljøflagg injiseres, slås aldri opp statisk» i AGENTS-backend.md.
 */
internal const val LOKAL_KAFKA_BROKER = "localhost:9092"

internal const val LOKAL_SCHEMA_REGISTRY = "mock://test"
