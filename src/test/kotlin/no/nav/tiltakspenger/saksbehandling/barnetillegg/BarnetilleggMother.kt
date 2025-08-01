package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

interface BarnetilleggMother {
    fun barnetillegg(
        begrunnelse: BegrunnelseVilkårsvurdering? = null,
        periode: Periode = ObjectMother.virkningsperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: SammenhengendePeriodisering<AntallBarn> = barnetilleggsPerioder(
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
        periode: Periode = ObjectMother.virkningsperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: SammenhengendePeriodisering<AntallBarn> = SammenhengendePeriodisering(
            antallBarn,
            periode,
        ),
    ): SammenhengendePeriodisering<AntallBarn> {
        return periodiseringAntallBarn
    }
}
