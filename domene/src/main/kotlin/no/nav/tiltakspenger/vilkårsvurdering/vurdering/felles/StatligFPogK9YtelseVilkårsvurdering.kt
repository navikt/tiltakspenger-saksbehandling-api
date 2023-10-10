package no.nav.tiltakspenger.vilkårsvurdering.vurdering.felles

import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.vedtak.ForeldrepengerVedtak
import no.nav.tiltakspenger.vilkårsvurdering.Utfall
import no.nav.tiltakspenger.vilkårsvurdering.Vurdering

abstract class StatligFPogK9YtelseVilkårsvurdering(
    val ytelser: List<ForeldrepengerVedtak>,
    val vurderingsperiode: Periode,
) : Vilkårsvurdering() {
    val ytelseVurderinger: List<Vurdering> = lagYtelseVurderinger(ytelser, vurderingsperiode, ytelseType(), kilde())

    override var manuellVurdering: Vurdering? = null

    abstract fun ytelseType(): List<ForeldrepengerVedtak.Ytelser>

    abstract fun kilde(): Kilde

    override fun vurderinger(): List<Vurdering> = (ytelseVurderinger + manuellVurdering).filterNotNull()

    override fun detIkkeManuelleUtfallet(): Utfall {
        val utfall = ytelseVurderinger.map { it.utfall }
        return when {
            utfall.any { it == Utfall.IKKE_OPPFYLT } -> Utfall.IKKE_OPPFYLT
            utfall.any { it == Utfall.KREVER_MANUELL_VURDERING } -> Utfall.KREVER_MANUELL_VURDERING
            else -> Utfall.OPPFYLT
        }
    }

    fun lagYtelseVurderinger(
        ytelser: List<ForeldrepengerVedtak>,
        vurderingsperiode: Periode,
        type: List<ForeldrepengerVedtak.Ytelser>,
        kilde: Kilde,
    ): List<Vurdering> = ytelser
        .filter {
            Periode(
                it.periode.fra,
                (it.periode.til),
            ).overlapperMed(vurderingsperiode)
        }
        .filter { it.ytelse in type }
        .map {
            Vurdering.KreverManuellVurdering(
                vilkår = vilkår(),
                kilde = Kilde.valueOf(it.kildesystem.name),
                fom = it.periode.fra,
                tom = it.periode.til,
                detaljer = "",
            )
        }.ifEmpty {
            listOf(
                Vurdering.Oppfylt(
                    vilkår = vilkår(),
                    kilde = kilde,
                    fom = vurderingsperiode.fra,
                    tom = vurderingsperiode.til,
                    detaljer = "",
                ),
            )
        }
}
