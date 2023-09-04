package no.nav.tiltakspenger.vilkårsvurdering.vurdering

import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.vedtak.ForeldrepengerVedtak
import no.nav.tiltakspenger.vedtak.ForeldrepengerVedtak.Ytelser.PLEIEPENGER_SYKT_BARN
import no.nav.tiltakspenger.vilkårsvurdering.Vilkår
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.felles.StatligFPogK9YtelseVilkårsvurdering

class PleiepengerSyktBarnVilkårsvurdering(
    ytelser: List<ForeldrepengerVedtak>,
    vurderingsperiode: Periode,
) : StatligFPogK9YtelseVilkårsvurdering(ytelser, vurderingsperiode) {
    override fun vilkår(): Vilkår = Vilkår.PLEIEPENGER_SYKT_BARN
    override fun ytelseType() = listOf(PLEIEPENGER_SYKT_BARN)
    override fun kilde() = Kilde.K9SAK
}
