package no.nav.tiltakspenger.domene

import no.nav.tiltakspenger.domene.fakta.Faktum

data class Vilkårsvurdering(
    val vilkår: Vilkår,
    val fakta: List<Faktum> = emptyList(),
    val vurderingsperiode: Periode,
    val utfallsperiodes: List<Utfallsperiode> = listOf(Utfallsperiode(utfall = Utfall.IkkeVurdert, periode = vurderingsperiode)),
) {
    fun vurder(faktum: Faktum): Vilkårsvurdering {
        val oppdaterteFakta = fakta + listOf(faktum).filter { it.erRelevantFor(vilkår) }
        return this.copy(
            utfallsperiodes = vilkår.vurder(oppdaterteFakta, vurderingsperiode),
            fakta = oppdaterteFakta,
        )
    }
}

//fun List<Vilkårsvurdering>.erInngangsVilkårOppfylt(): Boolean = this
//    .filter { it.vilkår.erInngangsVilkår }
//    .all { it.utfall is Utfall.VurdertOgOppfylt }

class Vilkårsvurderinger(
    val periode: Periode,
    val vilkårsvurderinger: List<Vilkårsvurdering>
) {
    // fun legg til vilkår
    // Sjekk at perioder er ok, at vilkår er gyldig ikke er to like på samme periode
    // fun
}