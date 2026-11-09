package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class OppdaterteTiltaksdeltakelser {
        private val deltakelse = tiltaksdeltakelse(fom = 1.januar(2026), tom = 31.januar(2026))
        private val innvilgelsesperioder = innvilgelsesperioder(
            periode = 1.januar(2026) til 31.januar(2026),
            valgtTiltaksdeltakelse = deltakelse,
        )

        @Test
        fun `er gyldig når tiltaksdeltakelsen dekker innvilgelsesperioden`() {
            val endretDeltakelse = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.HarSluttet)

            innvilgelsesperioder.ugyldigePerioder(Tiltaksdeltakelser(endretDeltakelse)) shouldBe null
            innvilgelsesperioder.medOppdaterteTiltaksdeltakelser(Tiltaksdeltakelser(endretDeltakelse)) shouldBe
                innvilgelsesperioder(periode = 1.januar(2026) til 31.januar(2026), valgtTiltaksdeltakelse = endretDeltakelse)
        }

        @Test
        fun `beholder innvilgelsesperioden når tiltaksdeltakelsen har krympet`() {
            val krympetDeltakelse = tiltaksdeltakelse(
                fom = 1.januar(2026),
                tom = 15.januar(2026),
                eksternTiltaksdeltakelseId = deltakelse.eksternDeltakelseId,
                internDeltakelseId = deltakelse.internDeltakelseId,
            )
            val tiltaksdeltakelser = Tiltaksdeltakelser(krympetDeltakelse)

            innvilgelsesperioder.ugyldigePerioder(tiltaksdeltakelser) shouldBe nonEmptyListOf(
                UgyldigInnvilgelsesperiode.TiltaksdeltakelsenDekkerIkkePerioden(
                    periode = 1.januar(2026) til 31.januar(2026),
                    internDeltakelseId = deltakelse.internDeltakelseId,
                    deltakelsesperiode = 1.januar(2026) til 15.januar(2026),
                ),
            )
            innvilgelsesperioder.medOppdaterteTiltaksdeltakelser(tiltaksdeltakelser) shouldBe innvilgelsesperioder
        }

        @Test
        fun `beholder innvilgelsesperioden når tiltaksdeltakelsen ikke kan innvilges eller er borte`() {
            val ikkeInnvilgbar = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart)

            innvilgelsesperioder.ugyldigePerioder(Tiltaksdeltakelser(ikkeInnvilgbar)) shouldBe nonEmptyListOf(
                UgyldigInnvilgelsesperiode.TiltaksdeltakelsenKanIkkeInnvilges(1.januar(2026) til 31.januar(2026), deltakelse.internDeltakelseId),
            )
            innvilgelsesperioder.medOppdaterteTiltaksdeltakelser(Tiltaksdeltakelser(ikkeInnvilgbar)) shouldBe innvilgelsesperioder

            innvilgelsesperioder.ugyldigePerioder(Tiltaksdeltakelser(emptyList())) shouldBe nonEmptyListOf(
                UgyldigInnvilgelsesperiode.TiltaksdeltakelsenFinnesIkke(1.januar(2026) til 31.januar(2026), deltakelse.internDeltakelseId),
            )
            innvilgelsesperioder.medOppdaterteTiltaksdeltakelser(Tiltaksdeltakelser(emptyList())) shouldBe innvilgelsesperioder
        }

        @Test
        fun `oppdaterer kun innvilgelsesperiodene som fortsatt er gyldige`() {
            val annenDeltakelse = tiltaksdeltakelse(fom = 1.februar(2026), tom = 28.februar(2026))
            val toPerioder = innvilgelsesperioder(
                innvilgelsesperiode(1.januar(2026) til 31.januar(2026), deltakelse),
                innvilgelsesperiode(1.februar(2026) til 28.februar(2026), annenDeltakelse),
            )
            val endretAnnenDeltakelse = annenDeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Fullført)

            val oppdatert = toPerioder.medOppdaterteTiltaksdeltakelser(Tiltaksdeltakelser(listOf(endretAnnenDeltakelse)))

            oppdatert shouldBe innvilgelsesperioder(
                innvilgelsesperiode(1.januar(2026) til 31.januar(2026), deltakelse),
                innvilgelsesperiode(1.februar(2026) til 28.februar(2026), endretAnnenDeltakelse),
            )
            toPerioder.ugyldigePerioder(Tiltaksdeltakelser(listOf(endretAnnenDeltakelse)))!!.map { it.internDeltakelseId } shouldBe
                listOf(deltakelse.internDeltakelseId)
        }
    }
}
