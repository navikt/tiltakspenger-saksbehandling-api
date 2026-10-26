package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import org.junit.jupiter.api.Test

class BarnSomFyller16Test {
    private val sisteInnvilgedeDag = 31.mars(2025)

    @Test
    fun `ett av to barn fyller 16 år i forlengelsen`() {
        BarnSomFyller16.utled(
            sisteInnvilgedeDag = sisteInnvilgedeDag,
            antallBarnIVedtaket = AntallBarn(2),
            antallBarn = listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.april(2025) til 10.april(2025)),
                PeriodeMedVerdi(AntallBarn(1), 11.april(2025) til 30.april(2025)),
            ).tilIkkeTomPeriodisering(),
        ) shouldBe BarnSomFyller16(antall = 1, antallBarnIVedtaket = 2, sisteDagMedBarnetillegg = 30.april(2025))
    }

    @Test
    fun `eneste barn fyller 16 år dagen etter vedtaket gir siste innvilgede dag som siste dag med barnetillegg`() {
        BarnSomFyller16.utled(
            sisteInnvilgedeDag = sisteInnvilgedeDag,
            antallBarnIVedtaket = AntallBarn(1),
            antallBarn = listOf(
                PeriodeMedVerdi(AntallBarn.ZERO, 1.april(2025) til 30.april(2025)),
            ).tilIkkeTomPeriodisering(),
        ) shouldBe BarnSomFyller16(antall = 1, antallBarnIVedtaket = 1, sisteDagMedBarnetillegg = sisteInnvilgedeDag)
    }

    @Test
    fun `ingen barn som fyller 16 år gir null`() {
        BarnSomFyller16.utled(
            sisteInnvilgedeDag = sisteInnvilgedeDag,
            antallBarnIVedtaket = AntallBarn(2),
            antallBarn = listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.april(2025) til 30.april(2025)),
            ).tilIkkeTomPeriodisering(),
        ) shouldBe null
    }

    @Test
    fun `flere barn i forlengelsen enn i vedtaket gir null`() {
        BarnSomFyller16.utled(
            sisteInnvilgedeDag = sisteInnvilgedeDag,
            antallBarnIVedtaket = AntallBarn(1),
            antallBarn = listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.april(2025) til 30.april(2025)),
            ).tilIkkeTomPeriodisering(),
        ) shouldBe null
    }
}
