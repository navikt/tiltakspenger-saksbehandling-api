package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import org.junit.jupiter.api.Test

class InnvilgelsesperioderTest {

    @Test
    fun `Skal krympe en innvilgelsesperiode`() {
        val innvilgelsesperioder = innvilgelsesperioder(1.januar(2026) til 31.januar(2026))
        val krympetPeriode = 15.januar(2026) til 31.januar(2026)

        val krympet = innvilgelsesperioder.krymp(listOf(krympetPeriode))

        krympet shouldBe Innvilgelsesperioder(
            listOf(innvilgelsesperioder.periodisering.verdier.first() to krympetPeriode),
        )
    }

    @Test
    fun `Skal krympe to innvilgelsesperioder innenfor en periode`() {
        val innvilgelsesperioder =
            innvilgelsesperioder(1.januar(2026) til 31.januar(2026), 2.februar(2026) til 28.februar(2026))
        val krympetPeriode = 15.januar(2026) til 15.februar(2026)

        val krympet = innvilgelsesperioder.krymp(listOf(krympetPeriode))

        krympet shouldBe Innvilgelsesperioder(
            listOf(
                innvilgelsesperioder.periodisering.verdier.first() to (15.januar(2026) til 31.januar(2026)),
                innvilgelsesperioder.periodisering.verdier[1] to (2.februar(2026) til 15.februar(2026)),
            ),
        )
    }

    @Test
    fun `Skal krympe to innvilgelsesperioder innenfor to perioder`() {
        val innvilgelsesperioder = innvilgelsesperioder(
            1.januar(2026) til 31.januar(2026),
            2.februar(2026) til 28.februar(2026),
        )
        val krympetTilPerioder = listOf(
            31.desember(2025) til 15.januar(2026),
            15.februar(2026) til 15.mars(2026),
        )

        val krympet = innvilgelsesperioder.krymp(krympetTilPerioder)

        krympet shouldBe Innvilgelsesperioder(
            listOf(
                innvilgelsesperioder.periodisering.verdier.first() to (1.januar(2026) til 15.januar(2026)),
                innvilgelsesperioder.periodisering.verdier[1] to (15.februar(2026) til 28.februar(2026)),
            ),
        )
    }

    @Test
    fun `Skal returnere null ved ingen overlapp med krympet periode`() {
        val innvilgelsesperioder =
            innvilgelsesperioder(1.januar(2026) til 31.januar(2026), 2.februar(2026) til 28.februar(2026))
        val krympetPeriode = 1.mars(2026) til 15.mars(2026)

        val krympet = innvilgelsesperioder.krymp(listOf(krympetPeriode))

        krympet shouldBe null
    }
}
