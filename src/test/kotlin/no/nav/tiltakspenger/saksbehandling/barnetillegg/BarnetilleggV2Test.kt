package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
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
    fun `krymping klipper valgte perioder og fjerner barn uten barnetillegg, slik at resultatet fortsatt er gyldig`() {
        val helePerioden = barn(fornavn = "Hele")
        val valgtPeriode = barn(fornavn = "Valgt", periode = 1.januar(2026) til 15.mars(2026))
        val bareIMars = barn(fornavn = "Mars", periode = 1 til 31.mars(2026))
        val fødtIMars = barn(fornavn = "Født", fødselsdato = 10.mars(2026))
        val barnetillegg = create(helePerioden, valgtPeriode, bareIMars, fødtIMars).getOrFail()

        val krympet = barnetillegg.krympTilPerioder(listOf(1.januar(2026) til 28.februar(2026)))!!

        krympet.barn shouldBe listOf(helePerioden, valgtPeriode.copy(periode = 1.januar(2026) til 28.februar(2026)))
        krympet.antallPerPeriode() shouldBe listOf((1.januar(2026) til 28.februar(2026)) to 2)
        BarnetilleggV2.create(krympet.barn, krympet.innvilgelsesperioder, null).getOrFail() shouldBe krympet
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
        create(barn(fnr = fnr), barn(fnr = Fnr.random()))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.SammeBarnFlereGanger>()
        create(barn(fornavn = "Ola"), barn(fornavn = "Kari")).getOrFail()
    }

    @Test
    fun `barn er samme barn når fnr er likt, eller når fødselsdato og navn er like`() {
        val fnr = ObjectMother.gyldigFnr()
        val ola = barn(fnr = fnr, fornavn = "Ola")

        ola.erSammeBarn(barn(fnr = fnr, fornavn = "Kari", fødselsdato = 2.januar(2020))) shouldBe true
        ola.erSammeBarn(barn(fornavn = " ola ")) shouldBe true
        ola.erSammeBarn(barn(fnr = Fnr.random(), fornavn = "OLA")) shouldBe true
        barn(fornavn = "Ola  Petter").erSammeBarn(barn(fornavn = "ola petter")) shouldBe true

        ola.erSammeBarn(barn(fnr = Fnr.random(), fornavn = "Kari")) shouldBe false
        ola.erSammeBarn(barn(fnr = Fnr.random(), fornavn = "Ola", fødselsdato = 2.januar(2020))) shouldBe false
        ola.erSammeBarn(barn(fornavn = "Ola", fødselsdato = 2.januar(2020))) shouldBe false
    }

    @Test
    fun `barn uten navn og med ulikt fnr er ikke samme barn`() {
        val utenNavn = barn(fnr = Fnr.random(), fornavn = null, etternavn = null, kilde = BarnMedBarnetillegg.Kilde.Søknad(SøknadId.random()))

        utenNavn.erSammeBarn(utenNavn.copy(fnr = Fnr.random())) shouldBe false
        utenNavn.erSammeBarn(utenNavn.copy(fnr = null)) shouldBe true
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
    fun `valgt periode mellom innvilgelsesperiodene gir ikke barnetillegg`() {
        val januar = 1 til 31.januar(2026)
        val mars = 1 til 31.mars(2026)

        create(barn(periode = 1 til 28.februar(2026)), innvilgelsesperioder = listOf(januar, mars))
            .leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.IngenDagerMedBarnetillegg>()

        create(barn(periode = 15.januar(2026) til 15.mars(2026)), innvilgelsesperioder = listOf(januar, mars))
            .getOrFail().antallPerPeriode() shouldBe listOf(
            (1 til 14.januar(2026)) to 0,
            (15 til 31.januar(2026)) to 1,
            (1 til 15.mars(2026)) to 1,
            (16 til 31.mars(2026)) to 0,
        )
    }

    @Test
    fun `barn fra søknaden må være fra en søknad i saken`() {
        val søknadId = SøknadId.random()
        val barn = listOf(barn(kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId)))

        barn.validerSøknader(setOf(søknadId)).getOrFail()
        barn.validerSøknader(setOf(SøknadId.random())).leftOrNull().shouldBeInstanceOf<UgyldigBarnetillegg.UkjentSøknad>()
    }

    @Test
    fun `exception-meldingen inneholder ikke barnets fødselsdato, som bare er i meldingen til saksbehandler`() {
        val feil = UgyldigBarnetillegg.ManglerNavn(barn(fødselsdato = 14.februar(2016)))

        feil.melding shouldContain "14. februar 2016"
        UgyldigBarnetilleggException(feil).message shouldBe "Ugyldig barnetillegg: ManglerNavn"
    }
}
