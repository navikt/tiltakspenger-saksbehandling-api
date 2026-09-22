package no.nav.tiltakspenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JobberTest {
    @Test
    fun `oppdatering av utbetalingsoversikt går lokalt og i dev, ikke i prod`() {
        oppdateringAvUtbetalingsoversiktErPåslått(isNais = false, erDev = false) shouldBe true
        oppdateringAvUtbetalingsoversiktErPåslått(isNais = false, erDev = true) shouldBe true
        oppdateringAvUtbetalingsoversiktErPåslått(isNais = true, erDev = true) shouldBe true
        oppdateringAvUtbetalingsoversiktErPåslått(isNais = true, erDev = false) shouldBe false
    }
}
