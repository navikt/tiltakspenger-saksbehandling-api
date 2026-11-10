package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import java.time.Instant

class HelvedStatusConsumerTest {
    private fun konsumer(
        fagsystem: String?,
        value: String? = """{"status":"OK"}""",
        key: String? = "utbetaling_1",
    ): List<Triple<String?, String?, Kafkametadata>> {
        val record = ConsumerRecord<String?, String?>("helved.status.v1", 2, 7, key, value)
        record.headers().add("traceparent", "abc".toByteArray())
        fagsystem?.let { record.headers().add("fagsystem", it.toByteArray()) }
        val lagret = mutableListOf<Triple<String?, String?, Kafkametadata>>()
        HelvedStatusConsumer.consume(record, fixedClock) { key, value, kafkametadata, _ ->
            lagret.add(Triple(key, value, kafkametadata))
        } shouldBe lagret.isNotEmpty()
        return lagret
    }

    @Test
    fun `lagrer meldinger for tiltakspenger rått med Kafka-metadata`() {
        konsumer("TILTAKSPENGER") shouldBe listOf(
            Triple(
                "utbetaling_1",
                """{"status":"OK"}""",
                Kafkametadata(
                    topic = "helved.status.v1",
                    partisjon = 2,
                    offset = 7,
                    tidspunkt = Instant.ofEpochMilli(ConsumerRecord.NO_TIMESTAMP),
                    tidspunktstype = "NoTimestampType",
                    headere = mapOf("traceparent" to "abc", "fagsystem" to "TILTAKSPENGER"),
                ),
            ),
        )
    }

    @Test
    fun `hopper over meldinger for andre fagsystemer`() {
        konsumer("AAP") shouldBe emptyList()
        konsumer(null) shouldBe emptyList()
    }

    @Test
    fun `lagrer meldinger for tiltakspenger uten innhold`() {
        konsumer("TILTAKSPENGER", value = null).single().second shouldBe null
        konsumer("TILTAKSPENGER", key = null).single().first shouldBe null
    }
}
