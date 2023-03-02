package no.nav.tiltakspenger.vilkårsvurdering.vurdering

import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.vedtak.YtelseSak
import no.nav.tiltakspenger.vilkårsvurdering.Vilkår
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.felles.StatligArenaYtelseVilkårsvurdering

class TiltakspengerVilkårsvurdering(
    ytelser: List<YtelseSak>,
    vurderingsperiode: Periode,
) : StatligArenaYtelseVilkårsvurdering(ytelser, vurderingsperiode) {
    override fun vilkår(): Vilkår = Vilkår.TILTAKSPENGER
    override fun ytelseType() = YtelseSak.YtelseSakYtelsetype.INDIV
}
