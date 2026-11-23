package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeMigrereTilOppdragsversjon3
import org.junit.jupiter.api.Test

class MigrerTilOppdragsversjon3ServiceTest {

    /** Mockene er strenge, så testen feiler hvis tjenesten leser eller skriver noe før den avviser kallet. */
    private val service = MigrerTilOppdragsversjon3Service(
        sakRepo = mockk(),
        utbetalingRepo = mockk(),
        utbetalingsklient = mockk(),
        v3Simuleringsklient = mockk(),
        clock = mockk(),
        sessionFactory = mockk(),
        erProd = true,
    )

    @Test
    fun `avviser migrering i prod uten å røre saken`() {
        runTest {
            service.migrer(SakId.random()) shouldBe KanIkkeMigrereTilOppdragsversjon3.FunksjonenErIkkePåslått.left()
        }
    }
}
