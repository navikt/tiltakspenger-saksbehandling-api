package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ArenaHendelseDTOTest {

    @Test
    fun `serialisering og deserialisering av ArenaKafkaMessage med after`() {
        val original = ArenaHendelseDTO(
            opType = ArenaOperationType.U,
            after = ArenaDeltakerDTO(
                DELTAKERSTATUSKODE = ArenaDeltakerStatusType.GJENN,
                DATO_FRA = "2024-10-14 00:00:00",
                DATO_TIL = "2025-08-10 00:00:00",
                PROSENT_DELTID = 50.0F,
                ANTALL_DAGER_PR_UKE = 2.0F,
                EKSTERN_ID = null,
            ),
        )

        val json = serialize(original)
        val deserialized = deserialize<ArenaHendelseDTO>(json)

        deserialized shouldBe original
    }

    @Test
    fun `serialisering og deserialisering av ArenaKafkaMessage uten after`() {
        val original = ArenaHendelseDTO(
            opType = ArenaOperationType.D,
            after = null,
        )

        val json = serialize(original)
        val deserialized = deserialize<ArenaHendelseDTO>(json)

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

        val deserialized = deserialize<ArenaHendelseDTO>(json)

        deserialized.opType shouldBe ArenaOperationType.U
        deserialized.after shouldBe ArenaDeltakerDTO(
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

        val deserialized = deserialize<ArenaHendelseDTO>(json)
        val reserialized = serialize(deserialized)
        val redeserialized = deserialize<ArenaHendelseDTO>(reserialized)

        redeserialized shouldBe deserialized
    }

    @Test
    fun `hendelse med deltakerinfo blir en hendelse for deltakeren uansett operasjon`() {
        val sakId = SakId.random()
        val tiltaksdeltakerId = TiltaksdeltakerId.random()

        ArenaOperationType.entries.forEach { opType ->
            val hendelse = ArenaHendelseDTO(opType = opType, after = deltakerinfo())
                .tilTiltaksdeltakerHendelse("TA123", sakId, tiltaksdeltakerId)
                .shouldNotBeNull()

            hendelse.eksternDeltakerId shouldBe "TA123"
            hendelse.sakId shouldBe sakId
            hendelse.internDeltakerId shouldBe tiltaksdeltakerId
        }
    }

    @Test
    fun `slettet deltakelse uten deltakerinfo gir ingen hendelse`() {
        ArenaHendelseDTO(opType = ArenaOperationType.D, after = null)
            .tilTiltaksdeltakerHendelse("TA123", SakId.random(), TiltaksdeltakerId.random())
            .shouldBeNull()
    }

    @ParameterizedTest
    @EnumSource(value = ArenaOperationType::class, names = ["I", "U"])
    fun `innsatt eller oppdatert deltakelse uten deltakerinfo er et kontraktsbrudd`(opType: ArenaOperationType) {
        shouldThrow<IllegalArgumentException> {
            ArenaHendelseDTO(opType = opType, after = null)
                .tilTiltaksdeltakerHendelse("TA123", SakId.random(), TiltaksdeltakerId.random())
        }
    }

    private fun deltakerinfo() = ArenaDeltakerDTO(
        DELTAKERSTATUSKODE = ArenaDeltakerStatusType.GJENN,
        DATO_FRA = "2024-10-14 00:00:00",
        DATO_TIL = "2025-08-10 00:00:00",
        PROSENT_DELTID = 50.0F,
        ANTALL_DAGER_PR_UKE = 2.0F,
        EKSTERN_ID = null,
    )
}
