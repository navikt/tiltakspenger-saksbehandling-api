package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingerTest {

    private val førstePeriode = Periode(6.januar(2025), 19.januar(2025))
    private val andrePeriode = Periode(20.januar(2025), 2.februar(2025))

    @Test
    fun `flere meldekortbehandlinger kan være åpne samtidig`() {
        val sakId = SakId.random()
        val behandlinger = listOf(
            ObjectMother.meldekortUnderBehandling(
                sakId = sakId,
                periode = førstePeriode,
                opprettet = 1.januar(2025).atStartOfDay(),
            ),
            ObjectMother.meldekortUnderBehandling(
                sakId = sakId,
                periode = andrePeriode,
                opprettet = 2.januar(2025).atStartOfDay(),
            ),
        )

        val meldekortbehandlinger = shouldNotThrowAny {
            Meldekortbehandlinger(verdi = behandlinger)
        }

        meldekortbehandlinger.åpneMeldekortbehandlinger shouldBe behandlinger
        meldekortbehandlinger.harÅpenBehandling shouldBe true
        meldekortbehandlinger.meldekortbehandlingerUnderBehandling.size shouldBe 2
    }

    @Test
    fun `to åpne meldekortbehandlinger kan ikke omfatte samme meldeperiodekjede`() {
        val sakId = SakId.random()
        shouldThrow<IllegalArgumentException> {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 2.januar(2025).atStartOfDay(),
                    ),
                ),
            )
        }.message shouldContain "To åpne meldekortbehandlinger kan ikke omfatte samme meldeperiodekjede"
    }

    @Test
    fun `en avbrutt behandling blokkerer ikke kjeden`() {
        val sakId = SakId.random()
        val kjedeId = MeldeperiodeKjedeId.fraPeriode(førstePeriode)
        val åpenBehandling = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            periode = førstePeriode,
            opprettet = 2.januar(2025).atStartOfDay(),
        )

        val meldekortbehandlinger = shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortbehandlingAvbrutt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                    ),
                    åpenBehandling,
                ),
            )
        }

        meldekortbehandlinger.hentÅpenBehandlingForKjede(kjedeId) shouldBe åpenBehandling
    }

    @Test
    fun `kjederMedAnnenÅpenBehandling ekskluderer behandlingen selv`() {
        val sakId = SakId.random()
        val kjedeId = MeldeperiodeKjedeId.fraPeriode(førstePeriode)
        val åpenBehandling = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            periode = førstePeriode,
            opprettet = 1.januar(2025).atStartOfDay(),
        )

        val meldekortbehandlinger = Meldekortbehandlinger(verdi = listOf(åpenBehandling))

        meldekortbehandlinger.kjederMedAnnenÅpenBehandling(listOf(kjedeId)) shouldBe setOf(kjedeId)
        meldekortbehandlinger.kjederMedAnnenÅpenBehandling(
            kjedeIder = listOf(kjedeId),
            ekskluderBehandlingId = åpenBehandling.id,
        ) shouldBe emptySet()
        meldekortbehandlinger.kjederMedAnnenÅpenBehandling(
            listOf(MeldeperiodeKjedeId.fraPeriode(andrePeriode)),
        ) shouldBe emptySet()
    }

    @Test
    fun `ingen åpne meldekortbehandlinger`() {
        val meldekortbehandlinger = Meldekortbehandlinger(verdi = emptyList())

        meldekortbehandlinger.åpneMeldekortbehandlinger.size shouldBe 0
        meldekortbehandlinger.harÅpenBehandling shouldBe false
    }

    @Test
    fun `første behandling på en kjede er FØRSTE_BEHANDLING og de påfølgende er KORRIGERING`() {
        val sakId = SakId.random()

        shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortBehandletManuelt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                    ObjectMother.meldekortBehandletManuelt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 2.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.KORRIGERING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 3.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.KORRIGERING,
                    ),
                ),
            )
        }
    }

    @Test
    fun `første behandling på en kjede kan ikke være KORRIGERING`() {
        val sakId = SakId.random()

        shouldThrow<IllegalArgumentException> {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.KORRIGERING,
                    ),
                ),
            )
        }.message shouldContain "Den første behandlingen av en meldeperiodekjede må ha type FØRSTE_BEHANDLING"
    }

    @Test
    fun `en påfølgende behandling på en kjede kan ikke være FØRSTE_BEHANDLING`() {
        val sakId = SakId.random()

        shouldThrow<IllegalArgumentException> {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortBehandletManuelt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 2.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                ),
            )
        }.message shouldContain "Den første behandlingen av en meldeperiodekjede må ha type FØRSTE_BEHANDLING"
    }

    @Test
    fun `en avbrutt behandling teller ikke med i typerekkefølgen`() {
        val sakId = SakId.random()

        shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortbehandlingAvbrutt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 2.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                ),
            )
        }
    }

    @Test
    fun `typerekkefølgen valideres per kjede`() {
        val sakId = SakId.random()

        shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortBehandletManuelt(
                        sakId = sakId,
                        periode = førstePeriode,
                        opprettet = 1.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                    ObjectMother.meldekortUnderBehandling(
                        sakId = sakId,
                        periode = andrePeriode,
                        opprettet = 2.januar(2025).atStartOfDay(),
                        type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                    ),
                ),
            )
        }
    }
}
