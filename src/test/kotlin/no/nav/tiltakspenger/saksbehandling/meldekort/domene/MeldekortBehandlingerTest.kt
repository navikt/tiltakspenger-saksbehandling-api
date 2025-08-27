package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class MeldekortBehandlingerTest {

    @Test
    fun `kun 1 eller 0 meldekortbehandling kan være UNDER_BEHANDLING om gangen`() {
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

    @Test
    fun `meldekortUnderBehandling inneholder kun meldekort med status UNDER_BEHANDLING`() {
        val sakId = SakId.random()
        val meldekortUnderBehandling = ObjectMother.meldekortUnderBehandling(sakId = sakId)
        val behandlinger = MeldekortBehandlinger(
            verdi = listOf(
                meldekortUnderBehandling,
                ObjectMother.meldekortUnderBehandling(
                    sakId = sakId,
                    status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                ),
                ObjectMother.meldekortBehandletAutomatisk(sakId = sakId),
                ObjectMother.meldekortBehandletManuelt(sakId = sakId),
            ),
        )

        behandlinger.meldekortUnderBehandling shouldBe meldekortUnderBehandling
    }

    @Test
    fun `meldekortBehandlingerSomErLagtTilbake inneholer kun meldekort som er KLAR_TIL_BEHANDLING`() {
        val sakId = SakId.random()
        val behandlingSomErLagtTilbake = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
        )

        val behandlinger = MeldekortBehandlinger(
            verdi = listOf(
                ObjectMother.meldekortUnderBehandling(sakId = sakId),
                behandlingSomErLagtTilbake,
                ObjectMother.meldekortBehandletAutomatisk(sakId = sakId),
                ObjectMother.meldekortBehandletManuelt(sakId = sakId),
            ),
        )

        behandlinger.meldekortBehandlingerSomErLagtTilbake shouldBe listOf(behandlingSomErLagtTilbake)
    }

    @Test
    fun `tillater kun 1 meldekort som er lagt tilbake per kjede`() {
        assertThrows<IllegalArgumentException> {
            val sakId = SakId.random()
            MeldekortBehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                    ),
                ),
            )
        }

        assertDoesNotThrow {
            val sakId = SakId.random()
            val periodeAndreMeldekortBehandling = Periode(20.januar(2025), 2.februar(2025))
            MeldekortBehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        kjedeId = MeldeperiodeKjedeId.fraPeriode(periodeAndreMeldekortBehandling),
                        status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                        periode = periodeAndreMeldekortBehandling,
                    ),
                ),
            )
        }
    }
}
