package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

class InnvilgbarPeriodeTest {

    @Test
    fun `gir hele perioden til tiltaksdeltakelsene som kan innvilges, sortert på periode`() {
        val senere = tiltaksdeltakelse(fom = 1.mars(2026), tom = 31.mars(2026))
        val tidligere = tiltaksdeltakelse(fom = 1.januar(2026), tom = 28.februar(2026))
        val overlappende = tiltaksdeltakelse(fom = 1.februar(2026), tom = 31.mars(2026))
        val ikkeStartet = tiltaksdeltakelse(fom = 1.januar(2026), tom = 31.mars(2026), status = TiltakDeltakerstatus.VenterPåOppstart)

        Tiltaksdeltakelser(listOf(senere, ikkeStartet, overlappende, tidligere)).innvilgbarePerioder() shouldBe listOf(
            InnvilgbarPeriode(tidligere.internDeltakelseId, 1.januar(2026) til 28.februar(2026)),
            InnvilgbarPeriode(overlappende.internDeltakelseId, 1.februar(2026) til 31.mars(2026)),
            InnvilgbarPeriode(senere.internDeltakelseId, 1.mars(2026) til 31.mars(2026)),
        )
    }
}
