package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import java.time.LocalDate

interface BarnetilleggMother {
    fun barnetillegg(
        begrunnelse: Begrunnelse? = null,
        periode: Periode = ObjectMother.vedtaksperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: IkkeTomPeriodisering<AntallBarn> = barnetilleggsPerioder(
            periode = periode,
            antallBarn = antallBarn,
        ),
    ): Barnetillegg {
        return BarnetilleggV1(
            periodisering = periodiseringAntallBarn,
            begrunnelse = begrunnelse,
        )
    }

    fun barnMedBarnetillegg(
        fnr: Fnr? = null,
        fornavn: String? = "Ola",
        etternavn: String? = "Nordmann",
        fødselsdato: LocalDate = 1.januar(2020),
        kilde: BarnMedBarnetillegg.Kilde = BarnMedBarnetillegg.Kilde.Saksbehandler,
        perioder: List<Periode>? = null,
    ): BarnMedBarnetillegg = BarnMedBarnetillegg(
        fnr = fnr,
        fornavn = fornavn,
        mellomnavn = null,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kilde = kilde,
        perioder = perioder?.toNonEmptyListOrNull(),
    )

    fun barnetilleggV2(
        begrunnelse: Begrunnelse? = null,
        innvilgelsesperioder: NonEmptyList<Periode> = nonEmptyListOf(ObjectMother.vedtaksperiode()),
        barn: List<BarnMedBarnetillegg> = listOf(barnMedBarnetillegg()),
    ): BarnetilleggV2 = BarnetilleggV2.create(
        barn = barn,
        innvilgelsesperioder = innvilgelsesperioder,
        begrunnelse = begrunnelse,
    ).getOrFail()

    /**
     * Barnetillegg for hele innvilgelsesperioden for barna i søknaden som har rett i den og som [velgBarn] velger, og for [andreBarn].
     * Brukes der søknaden opprettes i testen, siden søknadId først er kjent da.
     */
    fun barnetilleggForBarnISøknaden(
        innvilgelsesperioder: NonEmptyList<Periode>,
        begrunnelse: Begrunnelse? = null,
        velgBarn: (BarnetilleggFraSøknad) -> Boolean = { true },
        andreBarn: List<BarnMedBarnetillegg> = emptyList(),
    ): (Søknad) -> Barnetillegg = { søknad ->
        val totalPeriode = Periode(innvilgelsesperioder.minOf { it.fraOgMed }, innvilgelsesperioder.maxOf { it.tilOgMed })
        barnetilleggV2(
            begrunnelse = begrunnelse,
            innvilgelsesperioder = innvilgelsesperioder,
            barn = søknad.barnetillegg
                .filter { periodeMedRettPåBarnetillegg(it.fødselsdato).overlapperMed(totalPeriode) }
                .filter(velgBarn)
                .map { it.tilBarnMedBarnetillegg(søknad.id) } + andreBarn,
        )
    }

    fun barnetilleggsPerioder(
        periode: Periode = ObjectMother.vedtaksperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: IkkeTomPeriodisering<AntallBarn> = SammenhengendePeriodisering(
            antallBarn,
            periode,
        ),
    ): IkkeTomPeriodisering<AntallBarn> {
        return periodiseringAntallBarn
    }
}
