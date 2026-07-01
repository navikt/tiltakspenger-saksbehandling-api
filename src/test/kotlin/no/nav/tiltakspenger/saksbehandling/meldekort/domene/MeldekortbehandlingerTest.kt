package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingerTest {

    @Test
    fun `kun 1 eller 0 meldekortbehandling kan være åpen om gangen`() {
        val sakId = SakId.random()
        shouldThrow<IllegalArgumentException> {
            Meldekortbehandlinger(
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

        shouldNotThrowAny {
            Meldekortbehandlinger(verdi = listOf(ObjectMother.meldekortUnderBehandling()))
            Meldekortbehandlinger(verdi = emptyList())
        }
    }
}
