package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class MeldekortBehandletAutomatiskStatusTest {

    @Test
    fun `kun utfallene som sendes til manuell behandling logges som warn`() {
        MeldekortBehandletAutomatiskStatus.entries.filter { it.loggnivå == Level.WARN }.shouldContainExactly(
            MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING,
            MeldekortBehandletAutomatiskStatus.HAR_JUSTERING,
        )
    }
}
