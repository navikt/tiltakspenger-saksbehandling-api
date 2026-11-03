package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class TiltaksdeltakelseSomKanForlengesTest {

    @Test
    fun `gir tiltaksdeltakelsene som varer etter vedtaket, med antall dager fra siste periode de var innvilget, sist innvilget først`() {
        val tidligInnvilget = ObjectMother.tiltaksdeltakelse(1.januar(2025) til 30.juni(2025))
        val forkortet = ObjectMother.tiltaksdeltakelse(1.januar(2025) til 30.juni(2025))
        val sistInnvilget = ObjectMother.tiltaksdeltakelse(1.januar(2025) til 30.juni(2025))
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            ObjectMother.innvilgelsesperiode(1.januar(2025) til 15.januar(2025), tidligInnvilget, AntallDagerForMeldeperiode(4)),
            ObjectMother.innvilgelsesperiode(16.januar(2025) til 31.januar(2025), tidligInnvilget, AntallDagerForMeldeperiode(6)),
            ObjectMother.innvilgelsesperiode(1.februar(2025) til 28.februar(2025), forkortet),
            ObjectMother.innvilgelsesperiode(1.mars(2025) til 31.mars(2025), sistInnvilget, AntallDagerForMeldeperiode(10)),
        )
        val saksopplysninger = ObjectMother.saksopplysninger(
            fom = 1.januar(2025),
            tom = 30.juni(2025),
            tiltaksdeltakelse = listOf(
                tidligInnvilget,
                forkortet.copy(periode = 1.januar(2025) til 31.mars(2025)),
                sistInnvilget,
            ),
        )

        innvilgelsesperioder.tiltaksdeltakelserSomKanForlenges(
            forlengesFraOgMed = 1.april(2025),
            saksopplysninger = saksopplysninger,
        ) shouldBe listOf(
            TiltaksdeltakelseSomKanForlenges(sistInnvilget, AntallDagerForMeldeperiode(10)),
            TiltaksdeltakelseSomKanForlenges(tidligInnvilget, AntallDagerForMeldeperiode(6)),
        )
    }
}
