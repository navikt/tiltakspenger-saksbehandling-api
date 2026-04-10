package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import org.junit.jupiter.api.Test

class ArenaKafkaMessageTest {

    @Test
    fun `serialisering og deserialisering av ArenaKafkaMessage med after`() {
        val original = ArenaKafkaMessage(
            opType = OperationType.U,
            after = ArenaDeltakerKafka(
                DELTAKERSTATUSKODE = ArenaDeltakerStatusType.GJENN,
                DATO_FRA = "2024-10-14 00:00:00",
                DATO_TIL = "2025-08-10 00:00:00",
                PROSENT_DELTID = 50.0F,
                ANTALL_DAGER_PR_UKE = 2.0F,
                EKSTERN_ID = null,
            ),
        )

        val json = serialize(original)
        val deserialized = deserialize<ArenaKafkaMessage>(json)

        deserialized shouldBe original
    }

    @Test
    fun `serialisering og deserialisering av ArenaKafkaMessage uten after`() {
        val original = ArenaKafkaMessage(
            opType = OperationType.D,
            after = null,
        )

        val json = serialize(original)
        val deserialized = deserialize<ArenaKafkaMessage>(json)

        deserialized shouldBe original
    }

    @Test
    fun `deserialisering av ArenaKafkaMessage fra rå JSON med op_type`() {
        //language=json
        val json = """
            {
               "op_type": "U",
               "after": {
                 "ANTALL_DAGER_PR_UKE": 2.0,
                 "PROSENT_DELTID": 50.0,
                 "DELTAKERSTATUSKODE": "GJENN",
                 "DATO_FRA": "2024-10-14 00:00:00",
                 "DATO_TIL": "2025-08-10 00:00:00",
                 "EKSTERN_ID": null
               }
             }
        """.trimIndent()

        val deserialized = deserialize<ArenaKafkaMessage>(json)

        deserialized.opType shouldBe OperationType.U
        deserialized.after shouldBe ArenaDeltakerKafka(
            DELTAKERSTATUSKODE = ArenaDeltakerStatusType.GJENN,
            DATO_FRA = "2024-10-14 00:00:00",
            DATO_TIL = "2025-08-10 00:00:00",
            PROSENT_DELTID = 50.0F,
            ANTALL_DAGER_PR_UKE = 2.0F,
            EKSTERN_ID = null,
        )
    }

    @Test
    fun `deserialisering og reserialisering gir ekvivalent objekt`() {
        //language=json
        val json = """
            {
               "op_type": "I",
               "after": {
                 "ANTALL_DAGER_PR_UKE": 4.0,
                 "PROSENT_DELTID": 100.0,
                 "DELTAKERSTATUSKODE": "GJENN",
                 "DATO_FRA": "2024-01-01 00:00:00",
                 "DATO_TIL": "2024-12-31 00:00:00",
                 "EKSTERN_ID": "9bedf708-1aa2-4be0-a561-cbe60ff2e9f9"
               }
             }
        """.trimIndent()

        val deserialized = deserialize<ArenaKafkaMessage>(json)
        val reserialized = serialize(deserialized)
        val redeserialized = deserialize<ArenaKafkaMessage>(reserialized)

        redeserialized shouldBe deserialized
    }
}
