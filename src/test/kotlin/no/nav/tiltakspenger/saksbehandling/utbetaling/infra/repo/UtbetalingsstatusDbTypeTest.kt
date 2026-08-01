package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Statusene settes av svar fra utbetalingssystemet, så flere av dem ville krevd en konstruert feilsituasjon per variant for å nås fra en prodsti.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * Merk at `IKKE_PÅBEGYNT` lagres med `Å` — verdien står i basen allerede og kan ikke endres uten migrering.
 */
class UtbetalingsstatusDbTypeTest {

    @Test
    fun `statusene lagres med sitt avtalte navn`() {
        Utbetalingsstatus.entries.associateWith { it.toDbType() } shouldBe mapOf(
            Utbetalingsstatus.IkkePåbegynt to "IKKE_PÅBEGYNT",
            Utbetalingsstatus.SendtTilOppdrag to "SENDT_TIL_OPPDRAG",
            Utbetalingsstatus.FeiletMotOppdrag to "FEILET_MOT_OPPDRAG",
            Utbetalingsstatus.Ok to "OK",
            Utbetalingsstatus.OkUtenUtbetaling to "OK_UTEN_UTBETALING",
            Utbetalingsstatus.Avbrutt to "AVBRUTT",
        )
    }

    @Test
    fun `statusene leses tilbake fra lagret verdi`() {
        Utbetalingsstatus.entries.forEach {
            it.toDbType().toUtbetalingsstatus() shouldBe it
        }
    }

    @Test
    fun `null i kolonnen betyr ingen status`() {
        null.toUtbetalingsstatus() shouldBe null
    }

    @Test
    fun `ukjent verdi i kolonnen er en feil vi ikke skal svelge`() {
        shouldThrowWithMessage<IllegalArgumentException>("Ugyldig utbetalingsstatus: TULLESTATUS") {
            "TULLESTATUS".toUtbetalingsstatus()
        }
    }
}
