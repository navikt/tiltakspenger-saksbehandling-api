package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

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
        return Barnetillegg(
            periodisering = periodiseringAntallBarn,
            begrunnelse = begrunnelse,
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
