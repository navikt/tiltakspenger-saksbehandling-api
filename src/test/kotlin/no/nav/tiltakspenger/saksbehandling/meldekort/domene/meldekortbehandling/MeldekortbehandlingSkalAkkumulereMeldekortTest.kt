package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingSkalAkkumulereMeldekortTest {

    @Test
    fun `skalAkkumulereMeldekort kan være true for KLAR_TIL_BEHANDLING og UNDER_BEHANDLING`() {
        ObjectMother.meldekortUnderBehandling(
            status = MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            saksbehandler = null,
            skalAkkumulereMeldekort = true,
        ).skalAkkumulereMeldekort shouldBe true

        ObjectMother.meldekortUnderBehandling(
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
            skalAkkumulereMeldekort = true,
        ).skalAkkumulereMeldekort shouldBe true
    }

    @Test
    fun `skalAkkumulereMeldekort kan ikke være true for behandlede eller avbrutte meldekort`() {
        shouldThrow<IllegalArgumentException> {
            ObjectMother.meldekortBehandletManuelt().copy(skalAkkumulereMeldekort = true)
        }.message shouldContain "skalAkkumulereMeldekort kan kun være true"

        shouldThrow<IllegalArgumentException> {
            ObjectMother.meldekortBehandletAutomatisk().copy(skalAkkumulereMeldekort = true)
        }.message shouldContain "skalAkkumulereMeldekort kan kun være true"

        shouldThrow<IllegalArgumentException> {
            ObjectMother.meldekortbehandlingAvbrutt().copy(skalAkkumulereMeldekort = true)
        }.message shouldContain "skalAkkumulereMeldekort kan kun være true"
    }
}
