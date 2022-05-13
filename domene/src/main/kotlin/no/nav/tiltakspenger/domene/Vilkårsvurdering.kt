package no.nav.tiltakspenger.domene

sealed class Utfall {
    class IKKE_VURDERT: Utfall()
    class VURDERT_OG_OPPFYLT(val vilkårOppfyltPeriode: Periode): Utfall()
    class VURDERT_OG_IKKE_OPPFYLT: Utfall()
    class VURDERT_OG_TRENGER_MANUELL_VURDERING: Utfall()
}

data class Vilkårsvurdering(
    val utfall: Utfall = Utfall.IKKE_VURDERT(),
    val vilkår: Vilkår,
    val fakta: List<Faktum> = emptyList(),
    val vurderingsperiode: Periode
) {
    fun vurder(faktum: Faktum): Vilkårsvurdering {
        val oppdaterteFakta = fakta + listOf(faktum).filter { faktum -> faktum.erRelevantFor(vilkår) }
        return this.copy(
            utfall = vilkår.vurder(oppdaterteFakta, vurderingsperiode),
            fakta = oppdaterteFakta,
        )
    }
}

fun List<Vilkårsvurdering>.erInngangsVilkårOppfylt(): Boolean = this
    .filter { it.vilkår.erInngangsVilkår }
    .all { it.utfall is Utfall.VURDERT_OG_OPPFYLT }
