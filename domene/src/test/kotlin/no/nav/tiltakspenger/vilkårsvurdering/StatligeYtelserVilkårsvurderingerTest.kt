package no.nav.tiltakspenger.vilkårsvurdering

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.domene.Periode
import no.nav.tiltakspenger.domene.februar
import no.nav.tiltakspenger.domene.februarDateTime
import no.nav.tiltakspenger.domene.januarDateTime
import no.nav.tiltakspenger.objectmothers.ytelseSak
import no.nav.tiltakspenger.vedtak.YtelseSak
import org.junit.jupiter.api.Test

internal class StatligeYtelserVilkårsvurderingerTest {

    @Test
    fun `skal ha med alle`() {
        val vurderingsperiode = Periode(1.februar(2022), 20.februar(2022))
        val aapVilkårsvurdering = AAPVilkårsvurdering(
            ytelser = listOf(
                ytelseSak(
                    fomGyldighetsperiode = 1.januarDateTime(2022),
                    tomGyldighetsperiode = 31.januarDateTime(2022),
                    ytelsestype = YtelseSak.YtelseSakYtelsetype.AA,
                )
            ),
            vurderingsperiode = vurderingsperiode,
        )
        val dagpengerVilkårsvurdering = DagpengerVilkårsvurdering(
            ytelser = listOf(
                ytelseSak(
                    fomGyldighetsperiode = 1.februarDateTime(2022),
                    tomGyldighetsperiode = 28.februarDateTime(2022),
                    ytelsestype = YtelseSak.YtelseSakYtelsetype.DAGP,
                )
            ),
            vurderingsperiode = vurderingsperiode,
        )

        val statligeYtelserVilkårsvurderinger = StatligeYtelserVilkårsvurderinger(
            aap = aapVilkårsvurdering,
            dagpenger = dagpengerVilkårsvurdering,
        )

        statligeYtelserVilkårsvurderinger.samletUtfall() shouldBe Utfall.IKKE_OPPFYLT
        statligeYtelserVilkårsvurderinger.vurderinger().size shouldBe 13
    }

    @Test
    fun `Samlet utfall for statlige ytelser, hvis 1 er ikke godkjent er ingen godkjent`() {
        val vurderingsperiode = Periode(1.februar(2022), 20.februar(2022))
        val aapVilkårsvurdering = AAPVilkårsvurdering(
            ytelser = listOf(
                ytelseSak(
                    fomGyldighetsperiode = 1.januarDateTime(2022),
                    tomGyldighetsperiode = 31.januarDateTime(2022),
                    ytelsestype = YtelseSak.YtelseSakYtelsetype.AA,
                )
            ),
            vurderingsperiode = vurderingsperiode,
        )
        val dagpengerVilkårsvurdering = DagpengerVilkårsvurdering(
            ytelser = listOf(
                ytelseSak(
                    fomGyldighetsperiode = 1.februarDateTime(2022),
                    tomGyldighetsperiode = 28.februarDateTime(2022),
                    ytelsestype = YtelseSak.YtelseSakYtelsetype.DAGP,
                )
            ),
            vurderingsperiode = vurderingsperiode,
        )

        val statligeYtelserVilkårsvurderinger = StatligeYtelserVilkårsvurderinger(
            aap = aapVilkårsvurdering,
            dagpenger = dagpengerVilkårsvurdering,
        )

        statligeYtelserVilkårsvurderinger.samletUtfall() shouldBe Utfall.IKKE_OPPFYLT

    }

    @Test
    fun `Samlet utfall for statlige ytelser, hvis begge er godkjent er alle godkjent`() {
        val vurderingsperiode = Periode(1.februar(2022), 20.februar(2022))
        val aapVilkårsvurdering = AAPVilkårsvurdering(
            ytelser = emptyList(),
            vurderingsperiode = vurderingsperiode,
        )
        val dagpengerVilkårsvurdering = DagpengerVilkårsvurdering(
            ytelser = emptyList(),
            vurderingsperiode = vurderingsperiode,
        )

        val statligeYtelserVilkårsvurderinger = StatligeYtelserVilkårsvurderinger(
            aap = aapVilkårsvurdering,
            dagpenger = dagpengerVilkårsvurdering,
        )

        statligeYtelserVilkårsvurderinger.samletUtfall() shouldBe Utfall.OPPFYLT

    }
}
