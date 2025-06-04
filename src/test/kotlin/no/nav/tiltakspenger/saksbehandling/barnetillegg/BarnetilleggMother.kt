package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

interface BarnetilleggMother {
    fun barnetillegg(
        begrunnelse: BegrunnelseVilkårsvurdering? = null,
        periode: Periode = ObjectMother.virkningsperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: Periodisering<AntallBarn> = barnetilleggsPerioder(
            periode = periode,
            antallBarn = antallBarn,
        ),
    ): Barnetillegg {
        return Barnetillegg(
            periodisering = periodiseringAntallBarn,
            begrunnelse = null,
        )
    }

    fun barnetilleggsPerioder(
        periode: Periode = ObjectMother.virkningsperiode(),
        antallBarn: AntallBarn = AntallBarn(1),
        periodiseringAntallBarn: Periodisering<AntallBarn> = Periodisering(
            antallBarn,
            periode,
        ),
    ): Periodisering<AntallBarn> {
        return periodiseringAntallBarn
    }
}
