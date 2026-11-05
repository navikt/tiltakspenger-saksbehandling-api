package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetilleggV2Test {

    private val innvilgelsesperiode = 1.januar(2026) til 31.mars(2026)

    private fun barn(
        fødselsdato: LocalDate = 1.januar(2020),
        periode: Periode? = null,
        fnr: Fnr? = null,
        fornavn: String? = "Ola",
        etternavn: String? = "Nordmann",
        kilde: BarnMedBarnetillegg.Kilde = BarnMedBarnetillegg.Kilde.Saksbehandler,
    ) = BarnMedBarnetillegg(
        fnr = fnr,
        fornavn = fornavn,
        mellomnavn = null,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kilde = kilde,
        periode = periode,
    )

    private fun create(vararg barn: BarnMedBarnetillegg, innvilgelsesperioder: List<Periode> = listOf(innvilgelsesperiode)) =
        BarnetilleggV2.create(
            barn = barn.toList(),
            innvilgelsesperioder = nonEmptyListOf(innvilgelsesperioder.first(), *innvilgelsesperioder.drop(1).toTypedArray()),
            begrunnelse = null,
        )

    private fun BarnetilleggV2.antallPerPeriode() = periodisering.perioderMedVerdi.toList().map { it.periode to it.verdi.value }

    @Test
    fun `uten barn gir 0 barn i hele innvilgelsesperioden`() {
        create().getOrFail().antallPerPeriode() shouldBe listOf(innvilgelsesperiode to 0)
    }

    @Test
    fun `barn for hele perioden avkortes fra fødselen og når barnet fyller 16`() {
        val født = barn(fødselsdato = 15.februar(2026), fornavn = "Nyfødt")
        val fyller16 = barn(fødselsdato = 1.mars(2010), fornavn = "Stor")

        create(født, fyller16).getOrFail().antallPerPeriode() shouldBe listOf(
            (1.januar(2026) til 14.februar(2026)) to 1,
            (15.februar(2026) til 28.februar(2026)) to 2,
            (1.mars(2026) til 31.mars(2026)) to 1,
        )
    }

    @Test
    fun `barn med valgt periode gir barnetillegg bare i den perioden`() {
        create(
            barn(fornavn = "Ola"),
            barn(fornavn = "Kari", periode = 1 til 31.januar(2026)),
        ).getOrFail().antallPerPeriode() shouldBe listOf(
            (1 til 31.januar(2026)) to 2,
            (1.februar(2026) til 31.mars(2026)) to 1,
        )
    }

    @Test
    fun `gir ikke barnetillegg mellom innvilgelsesperiodene`() {
        val januar = 1 til 31.januar(2026)
        val mars = 1 til 31.mars(2026)

        val barnetillegg = create(barn(), innvilgelsesperioder = listOf(mars, januar)).getOrFail()

        barnetillegg.innvilgelsesperioder shouldBe nonEmptyListOf(januar, mars)
        barnetillegg.periodisering.perioderMedVerdi.toList() shouldBe listOf(
            PeriodeMedVerdi(AntallBarn(1), januar),
            PeriodeMedVerdi(AntallBarn(1), mars),
        )
    }

    @Test
    fun `krymping beholder barna`() {
        val barnetillegg = create(barn()).getOrFail()

        val krympet = barnetillegg.krympTilPerioder(listOf(1.februar(2026) til 30.april(2026)))!!

        krympet.barn shouldBe barnetillegg.barn
        krympet.antallPerPeriode() shouldBe listOf((1.februar(2026) til 31.mars(2026)) to 1)
        barnetillegg.krympTilPerioder(listOf(1 til 30.april(2026))) shouldBe null
    }

    @Test
    fun `barn lagt til av saksbehandler må ha fornavn og etternavn`() {
        create(barn(etternavn = " ")).leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.ManglerNavn>()
        create(barn(fornavn = null, kilde = BarnMedBarnetillegg.Kilde.Søknad(SøknadId.random()))).getOrFail()
    }

    @Test
    fun `samme barn kan ikke velges flere ganger`() {
        val fnr = ObjectMother.gyldigFnr()
        create(barn(fnr = fnr, fornavn = "Ola"), barn(fnr = fnr, fornavn = "Kari"))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.SammeBarnFlereGanger>()
        create(barn(fornavn = "Ola"), barn(fornavn = " ola "))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.SammeBarnFlereGanger>()
        create(barn(fornavn = "Ola"), barn(fornavn = "Kari")).getOrFail()
    }

    @Test
    fun `valgt periode må være innenfor innvilgelsen og barnets rett`() {
        create(barn(periode = 1.januar(2026) til 1.april(2026)))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.PeriodeUtenforInnvilgelsen>()
        create(barn(fødselsdato = 1.februar(2026), periode = 1 til 31.januar(2026)))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.PeriodeUtenforBarnetsRett>()
    }

    @Test
    fun `barn uten rett i innvilgelsesperioden gir feil`() {
        create(barn(fødselsdato = 1.april(2026)))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.IngenDagerMedBarnetillegg>()
        create(barn(fødselsdato = 1.januar(2010)))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.IngenDagerMedBarnetillegg>()
    }

    @Test
    fun `barn fra søknaden må være fra en søknad i saken`() {
        val søknadId = SøknadId.random()
        val barn = listOf(barn(kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId)))

        barn.validerSøknader(setOf(søknadId)).getOrFail()
        barn.validerSøknader(setOf(SøknadId.random())).leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.UkjentSøknad>()
    }
}
