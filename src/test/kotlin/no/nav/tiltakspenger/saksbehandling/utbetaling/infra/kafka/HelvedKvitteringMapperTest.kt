package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

class HelvedKvitteringMapperTest {
    private val referanse = Utbetalingsreferanse("0123456789ABCDE")
    private val detaljer = """{"ytelse":"TILTAKSPENGER","linjer":[]}"""

    @Test
    fun `mapper status referanser og eksplisitte simuleringer`() {
        mapOf(
            """{"status":"MOTTATT","detaljer":$detaljer}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.IkkePåbegynt, emptySet()),
            """{"status":"HOS_OPPDRAG","detaljer":$detaljer}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.SendtTilOppdrag, emptySet()),
            """{"status":"OK","detaljer":$detaljer}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.Ok, emptySet()),
            """{"status":"OK"}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.OkUtenUtbetaling, emptySet()),
            """{"status":"OK","detaljer":null}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.OkUtenUtbetaling, emptySet()),
            """{"status":"FEILET","error":{"statusCode":400,"msg":"feil"}}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.FeiletMotOppdrag, emptySet()),
            """{"status":"FEILET","error":{"statusCode":500,"msg":"feil"}}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.FeiletMotOppdrag, emptySet()),
            """{"status":"OK","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[{"behandlingId":"${referanse.verdi}"}]}}""" to
                Utbetalingskvittering.Status(Utbetalingsstatus.Ok, setOf(referanse)),
            """{"status":"OK","simulering":true}""" to Utbetalingskvittering.Simulering(emptySet()),
        ).forEach { (json, forventet) -> tilUtbetalingsstatus(json).getOrFail() shouldBe forventet }
    }

    @Test
    fun `ukjent og ugyldig innhold er modellerte permanente feil`() {
        listOf(
            """{"status":"NY"}""",
            """{"status":7}""",
            """{"status":"OK","detaljer":[]}""",
            """{"status":"OK","detaljer":{"linjer":[{"behandlingId":"for-kort"}]}}""",
            """{"status":"OK","detaljer":{"linjer":[{}]}}""",
            """{"status":"OK","simulering":"true"}""",
            """{"status":"OK","simulering":null}""",
            "{}",
            "null",
            "[]",
            "ikke json",
        ).forEach { json ->
            tilUtbetalingsstatus(json).isLeft() shouldBe true
            tilUtbetalingsstatus(json).leftOrNull()!!.forbigående shouldBe false
        }
    }
}
