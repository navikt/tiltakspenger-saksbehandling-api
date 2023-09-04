package no.nav.tiltakspenger.vilkårsvurdering.vurdering

import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.felles.toDisplayDate
import no.nav.tiltakspenger.vedtak.UføreVedtak
import no.nav.tiltakspenger.vilkårsvurdering.Utfall
import no.nav.tiltakspenger.vilkårsvurdering.Vilkår
import no.nav.tiltakspenger.vilkårsvurdering.Vurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.felles.Vilkårsvurdering
import java.time.LocalDate

data class UføreVilkarsvurdering(
    private val uføreVedtak: UføreVedtak?,
    private val vurderingsperiode: Periode,
) : Vilkårsvurdering() {
    val uføreVurderinger: List<Vurdering> = lagVurderingerFraVedtak()
    override fun vilkår(): Vilkår = Vilkår.UFØRETRYGD

    override var manuellVurdering: Vurdering? = null

    override fun vurderinger(): List<Vurdering> = (uføreVurderinger + manuellVurdering).filterNotNull()
    override fun detIkkeManuelleUtfallet(): Utfall {
        val utfall = uføreVurderinger.map { it.utfall }
        return when {
            utfall.any { it == Utfall.IKKE_OPPFYLT } -> Utfall.IKKE_OPPFYLT
            utfall.any { it == Utfall.KREVER_MANUELL_VURDERING } -> Utfall.KREVER_MANUELL_VURDERING
            else -> Utfall.OPPFYLT
        }
    }

    private fun lagVurderingerFraVedtak(): List<Vurdering> {
        val dato = if (
            (uføreVedtak == null) or
            (uføreVedtak?.harUføregrad == false) or
            (uføreVedtak?.virkDato == null)
        ) {
            LocalDate.of(9999, 12, 31)
        } else {
            uføreVedtak!!.virkDato!!
        }

        if (vurderingsperiode.før(dato)) {
            return listOf(
                Vurdering.Oppfylt(
                    vilkår = Vilkår.UFØRETRYGD,
                    kilde = Kilde.PESYS,
                    detaljer = if (dato.toString() == LocalDate.of(9999, 12, 31)
                            .toString()
                    ) {
                        ""
                    } else {
                        "Uførevedtak fra ${dato.toDisplayDate()}"
                    },
                ),
            )
        }

        if (vurderingsperiode.inneholder(dato)) {
            return listOf(
                // Vi trenger ikke den oppfylte når det finnes en ikke_oppfylt.
                // Da er den oppfylte implisit
//                Vurdering(
//                    vilkår = Vilkår.UFØRETRYGD,
//                    kilde = KILDE,
//                    fom = vurderingsperiode.fra,
//                    tom = dato.minusDays(1),
//                    utfall = Utfall.OPPFYLT,
//                    detaljer = "",
//                ),
                Vurdering.KreverManuellVurdering(
                    vilkår = Vilkår.UFØRETRYGD,
                    kilde = Kilde.PESYS,
                    fom = dato,
                    tom = vurderingsperiode.til,
                    detaljer = "",
                ),
            )
        }

        return listOf(
            Vurdering.KreverManuellVurdering(
                vilkår = Vilkår.UFØRETRYGD,
                kilde = Kilde.PESYS,
                fom = dato,
                tom = vurderingsperiode.til,
                detaljer = "",
            ),
        )
    }
}
