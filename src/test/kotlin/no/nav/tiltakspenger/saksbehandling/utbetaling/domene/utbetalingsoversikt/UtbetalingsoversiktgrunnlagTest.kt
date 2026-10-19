package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test

class UtbetalingsoversiktgrunnlagTest {
    private val fnr = ObjectMother.gyldigFnr()
    private val august = Periode(1.august(2025), 31.august(2025))
    private val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(august))

    @Test
    fun `en ytelse vi samordner med, i perioden, med personen som rettighetshaver, er innenfor`() {
        grunnlag.avgrensningsårsak(Ytelsetype.DAGPENGER, Periode(25.august(2025), 7.september(2025)), fnr) shouldBe null
        grunnlag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, august, fnr) shouldBe null
    }

    @Test
    fun `årsakene prøves i fast rekkefølge`() {
        grunnlag.avgrensningsårsak(null, august, fnr) shouldBe Avgrensningsårsak.UTEN_YTELSESTYPE
        grunnlag.avgrensningsårsak(Ytelsetype.UKJENT, august, fnr) shouldBe Avgrensningsårsak.ANNEN_YTELSESTYPE
        grunnlag.avgrensningsårsak(Ytelsetype.DAGPENGER, Periode(1.juli(2025), 31.juli(2025)), fnr) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
        grunnlag.avgrensningsårsak(Ytelsetype.DAGPENGER, august, ObjectMother.gyldigFnr()) shouldBe Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
        grunnlag.avgrensningsårsak(Ytelsetype.DAGPENGER, august, null) shouldBe Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
    }

    @Test
    fun `tiltakspenger på fredagen før en periode som starter i helgen, beholdes`() {
        val grunnlagFraLørdag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(1.november(2025), 30.november(2025))))
        val grunnlagFraSøndag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(1.februar(2026), 28.februar(2026))))

        grunnlagFraLørdag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(31.oktober(2025), 31.oktober(2025)), fnr) shouldBe null
        grunnlagFraSøndag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(30.januar(2026), 30.januar(2026)), fnr) shouldBe null
        grunnlagFraLørdag.perioder shouldBe listOf(Periode(1.november(2025), 30.november(2025)))
    }

    @Test
    fun `hver periode som starter i helgen, tar med fredagen før`() {
        val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(1.september(2025), 10.september(2025)), Periode(20.september(2025), 30.september(2025))))

        grunnlag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(19.september(2025), 19.september(2025)), fnr) shouldBe null
        grunnlag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(18.september(2025), 18.september(2025)), fnr) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
    }

    @Test
    fun `fredagen før tas bare med for tiltakspenger og bare når perioden starter i helgen`() {
        val grunnlagFraSøndag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(1.februar(2026), 28.februar(2026))))

        grunnlagFraSøndag.avgrensningsårsak(Ytelsetype.DAGPENGER, Periode(30.januar(2026), 30.januar(2026)), fnr) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
        grunnlagFraSøndag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(29.januar(2026), 29.januar(2026)), fnr) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
        grunnlag.avgrensningsårsak(Ytelsetype.TILTAKSPENGER, Periode(31.juli(2025), 31.juli(2025)), fnr) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
    }

    @Test
    fun `grunnlaget må ha minst én periode`() {
        shouldThrow<IllegalArgumentException> { Utbetalingsoversiktgrunnlag(fnr, emptyList()) }
    }

    @Test
    fun `grunnlaget for en sak uten utbetalinger er behandlingsgrunnlagsperiodene`() {
        val vedtaksperiode = Periode(1.januar(2025), 31.januar(2025))
        val (sak) = ObjectMother.nySakMedVedtak(vedtaksperiode = vedtaksperiode)

        val grunnlag = Utbetalingsoversiktgrunnlag.forSak(sak)

        grunnlag.fnr shouldBe sak.fnr
        sak.utbetalinger.perioder shouldBe emptyList()
        grunnlag.perioder shouldBe sak.behandlingsgrunnlagsperioder!!.perioder
        grunnlag.perioder.any { it.inneholderHele(vedtaksperiode) } shouldBe true
    }

    @Test
    fun `utbetalingsperioder som begynner før behandlingsgrunnlagsperiodene, utvider ikke grunnlaget`() {
        withTestApplicationContext { tac ->
            val sak = tac.førsteMeldekortIverksatt(fnr = ObjectMother.gyldigFnr())
            val behandlingsgrunnlagsperioder = sak.behandlingsgrunnlagsperioder!!.perioder

            sak.utbetalinger.perioder.first().fraOgMed shouldBeBefore behandlingsgrunnlagsperioder.first().fraOgMed
            Utbetalingsoversiktgrunnlag.forSak(sak).perioder shouldBe behandlingsgrunnlagsperioder
        }
    }
}
