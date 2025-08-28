package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class MeldekortBehandlingerTest {

    @Test
    fun `kun 1 eller 0 meldekortbehandling kan være åpen om gangen`() {
        val sakId = SakId.random()
        assertThrows<IllegalArgumentException> {
            MeldekortBehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                    ),
                ),
            )
        }

        assertDoesNotThrow {
            MeldekortBehandlinger(verdi = listOf(ObjectMother.meldekortUnderBehandling()))
            MeldekortBehandlinger(verdi = emptyList())
        }
    }
}
