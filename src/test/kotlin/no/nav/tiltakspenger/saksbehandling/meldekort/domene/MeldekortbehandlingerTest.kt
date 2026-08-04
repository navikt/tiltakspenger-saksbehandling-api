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
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingerTest {

    private val førstePeriode = Periode(6.januar(2025), 19.januar(2025))
    private val andrePeriode = Periode(20.januar(2025), 2.februar(2025))

    @Test
    fun `flere meldekortbehandlinger kan vaere aapne samtidig`() {
        val sakId = SakId.random()
        val meldekortbehandlinger = shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
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
                ),
            )
        }

        meldekortbehandlinger.åpneMeldekortbehandlinger.size shouldBe 2
        meldekortbehandlinger.harÅpenBehandling shouldBe true
        meldekortbehandlinger.meldekortbehandlingerUnderBehandling.size shouldBe 2
        meldekortbehandlinger.kjedeIderMedÅpenBehandling shouldBe setOf(
            MeldeperiodeKjedeId.fraPeriode(førstePeriode),
            MeldeperiodeKjedeId.fraPeriode(andrePeriode),
        )
    }

    @Test
    fun `to aapne meldekortbehandlinger kan ikke omfatte samme meldeperiodekjede`() {
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

        val meldekortbehandlinger = shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortbehandlingAvbrutt(
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
        }

        meldekortbehandlinger.kjedeIderMedÅpenBehandling shouldBe setOf(kjedeId)
    }

    @Test
    fun `kjederMedAnnenAapenBehandling ekskluderer behandlingen selv`() {
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
    fun `ingen aapne meldekortbehandlinger`() {
        val meldekortbehandlinger = Meldekortbehandlinger(verdi = emptyList())

        meldekortbehandlinger.åpneMeldekortbehandlinger.size shouldBe 0
        meldekortbehandlinger.harÅpenBehandling shouldBe false
    }
}
