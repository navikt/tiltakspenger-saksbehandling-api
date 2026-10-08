package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import org.junit.jupiter.api.Test

class UtbetalingsoversiktgrunnlagTest {
    private val fnr = ObjectMother.gyldigFnr()
    private val august = Periode(1.august(2025), 31.august(2025))
    private val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(august))

    @Test
    fun `en ytelse vi samordner med, i perioden, med personen som rettighetshaver, er innenfor`() {
        grunnlag.avgrensningsårsak("Dagpenger", Periode(25.august(2025), 7.september(2025)), fnr.verdi) shouldBe null
        grunnlag.avgrensningsårsak("Tiltakspenger", august, fnr.verdi) shouldBe null
    }

    @Test
    fun `årsakene prøves i fast rekkefølge`() {
        grunnlag.avgrensningsårsak(null, august, fnr.verdi) shouldBe Avgrensningsårsak.UTEN_YTELSESTYPE
        grunnlag.avgrensningsårsak("Bidragsforskudd", august, fnr.verdi) shouldBe Avgrensningsårsak.ANNEN_YTELSESTYPE
        grunnlag.avgrensningsårsak("Ukjent", august, fnr.verdi) shouldBe Avgrensningsårsak.ANNEN_YTELSESTYPE
        grunnlag.avgrensningsårsak("Dagpenger", Periode(1.juli(2025), 31.juli(2025)), fnr.verdi) shouldBe Avgrensningsårsak.UTENFOR_PERIODENE
        grunnlag.avgrensningsårsak("Dagpenger", august, ObjectMother.gyldigFnr().verdi) shouldBe Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
        grunnlag.avgrensningsårsak("Dagpenger", august, null) shouldBe Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
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
