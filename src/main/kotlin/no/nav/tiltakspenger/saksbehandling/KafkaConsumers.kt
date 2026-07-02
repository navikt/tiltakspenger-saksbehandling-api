package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.ktor.common.oppstart.KafkaConsumerOppsett
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext

internal fun kafkaConsumers(
    isNais: Boolean,
    applicationContext: ApplicationContext,
): List<KafkaConsumerOppsett> = buildList {
    if (isNais) {
        add(
            KafkaConsumerOppsett(
                navn = "tiltaksdeltaker-arena-consumer",
                start = { applicationContext.tiltaksdeltakerArenaConsumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "tiltaksdeltaker-komet-consumer",
                start = { applicationContext.tiltaksdeltakerKometConsumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "tiltaksdeltaker-team-tiltak-consumer",
                start = { applicationContext.tiltaksdeltakerTeamTiltakConsumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "leesah-consumer",
                start = { applicationContext.leesahConsumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "aktor-v2-consumer",
                start = { applicationContext.aktorV2Consumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "kabal-klagehendelse-consumer",
                start = { applicationContext.kabalKlagehendelseConsumer.run() },
                stopp = {},
            ),
        )
        add(
            KafkaConsumerOppsett(
                navn = "tilbakekreving-consumer",
                start = { applicationContext.tilbakekrevingConsumer.run() },
                stopp = {},
            ),
        )
    }
}
