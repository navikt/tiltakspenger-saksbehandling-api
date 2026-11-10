package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import no.nav.tiltakspenger.libs.kafka.infra.Producer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer

/**
 * Sender ferdigserialiserte V3-utbetalinger til Hel Ved på `tpts.utbetaling-v1` med utbetalings-id som Kafka-nøkkel.
 *
 * Kildekode: https://github.com/navikt/helved-utbetaling/blob/main/apps/abetal/main/abetal/Kafka.kt
 * Dokumentasjon: https://helved-docs.intern.dev.nav.no/kom_i_gang/opprett_utbetaling
 * API-spec (AsyncAPI): https://github.com/navikt/helved-docs/blob/main/public/asyncapi-spec.yml
 * Meldingsmodell: https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/TpDto.kt
 * Slack: [#team-hel-ved](https://nav-it.slack.com/archives/C06SJTR2X3L)
 * Teamkatalog: https://teamkatalogen.nav.no/team/93e78ae9-babc-452c-965d-e7dc67af8612
 */
class UtbetalingKafkaProducer(
    private val topic: String,
    private val kafkaProducer: Producer<String, String>,
) : UtbetalingProducer {
    override fun produser(utbetalingId: UtbetalingId, helvedUtbetalingJson: String) =
        kafkaProducer.produce(topic, utbetalingId.toString(), helvedUtbetalingJson)
}
