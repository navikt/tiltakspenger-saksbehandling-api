package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingGyldigeKommandoerExTest {

    /**
     * Leseroller (veileder, utvikler) kan se behandlingen, men ikke utføre kommandoer på den.
     * De får derfor alltid en tom liste, uavhengig av behandlingens tilstand.
     */
    @Test
    fun `bruker uten saksbehandler- eller beslutterrolle får ingen gyldige kommandoer`() {
        val behandling = ObjectMother.meldekortUnderBehandling()

        listOf(ObjectMother.saksbehandlerUtenTilgang(), ObjectMother.veileder(), ObjectMother.utvikler()).forEach { bruker ->
            behandling.finnGyldigeKommandoer(bruker) shouldBe emptyList()
        }
    }
}
