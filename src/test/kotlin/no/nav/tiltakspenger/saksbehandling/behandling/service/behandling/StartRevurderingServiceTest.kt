package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class StartRevurderingServiceTest {

    @Test
    fun `forlengelse kan ikke startes når den ikke er påslått`() = runTest {
        val sak = ObjectMother.nySak()
        val service = StartRevurderingService(
            sakService = mockk(),
            rammebehandlingRepo = mockk(),
            hentSaksopplysingerService = mockk(),
            clock = fixedClock,
            statistikkService = mockk(),
            sessionFactory = mockk(),
            forlengelseErPåslått = false,
        )

        service.forberedRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sak.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = ObjectMother.saksbehandler(),
                revurderingType = StartRevurderingType.FORLENGELSE,
                vedtakIdSomOmgjøres = null,
                vedtakIdSomForlenges = VedtakId.random(),
                klagebehandlingId = null,
            ),
            sak = sak,
        ).leftOrNull() shouldBe KanIkkeStarteRevurdering.ForlengelseErIkkePåslått
    }
}
