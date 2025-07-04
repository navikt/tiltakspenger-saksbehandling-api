package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger.DagSammenligning
import java.time.LocalDate

data class SammenligningAvBeregninger(
    val meldeperiode: List<MeldeperiodeSammenligninger>,
) {
    data class MeldeperiodeSammenligninger(
        val periode: Periode,
        val differanseFraForrige: Int,
        val dager: List<DagSammenligning>,
    )

    data class DagSammenligning(
        val dato: LocalDate,
        val status: ForrigeOgGjeldende<MeldeperiodeBeregningDag>,
        val beløp: ForrigeOgGjeldende<Int>,
        val barnetillegg: ForrigeOgGjeldende<Int>,
        val prosent: ForrigeOgGjeldende<Int>,
    ) {
        val erEndret: Boolean by lazy { beløp.erEndret || barnetillegg.erEndret }

        /** Ordinær + barnetillegg */
        val nyttTotalbeløp: Int by lazy {
            beløp.gjeldende + barnetillegg.gjeldende
        }

        /** Ordinær + barnetillegg */
        val forrigeTotalbeløp by lazy {
            (beløp.forrige ?: 0) + (barnetillegg.forrige ?: 0)
        }
        val totalbeløpEndring = nyttTotalbeløp - forrigeTotalbeløp
    }

    /**
     * Holder på forrige og gjeldende verdi for en gitt type. Gjeldende verdi er enten nåværende tilstand
     * eller verdien som ble gjeldende etter en endring
     */
    data class ForrigeOgGjeldende<T>(
        val forrige: T?,
        val gjeldende: T,
    ) {
        val erEndret: Boolean by lazy { forrige != gjeldende }
    }
}

fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregning?,
    gjeldendeBeregning: MeldeperiodeBeregning,
): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
    // I de fleste cases er det ikke gjort noen korrigering, dermed er det bare nåtilstand som gjelder og det vil ikke finnes noen  "forrige" beregning
    if (forrigeBeregning == null) {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = gjeldendeBeregning.periode,
            differanseFraForrige = 0,
            dager = gjeldendeBeregning.dager.map {
                DagSammenligning(
                    dato = it.dato,
                    status = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it,
                    ),
                    beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.beløp,
                    ),
                    barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.beløpBarnetillegg,
                    ),
                    prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.prosent,
                    ),
                )
            },
        )
    }
    // Hvis det er gjort en korrigering, så må vi sammenligne forrige og gjeldende beregning (det vil si de korrigerte verdiene)
    require(forrigeBeregning.periode == gjeldendeBeregning.periode) { "Periodene må være like" }

    return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
        periode = forrigeBeregning.periode,
        differanseFraForrige = gjeldendeBeregning.totalBeløp - forrigeBeregning.totalBeløp,
        dager = forrigeBeregning.dager
            .zip(gjeldendeBeregning.dager)
            .map { (forrige, gjeldende) -> sammenlign(forrige, gjeldende) },
    )
}

private fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregningDag,
    gjeldendeBeregning: MeldeperiodeBeregningDag,
): DagSammenligning {
    require(forrigeBeregning.dato == gjeldendeBeregning.dato) { "Datoene må være like" }

    return DagSammenligning(
        dato = forrigeBeregning.dato,
        status = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning,
            gjeldende = gjeldendeBeregning,
        ),
        beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløp,
            gjeldende = gjeldendeBeregning.beløp,
        ),
        barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløpBarnetillegg,
            gjeldende = gjeldendeBeregning.beløpBarnetillegg,
        ),
        prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.prosent,
            gjeldende = gjeldendeBeregning.prosent,
        ),
    )
}
