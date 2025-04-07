package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskUkedagOgDatoUtenÅrFormatter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger.DagSammenligning

data class SammenligningAvBeregninger(
    val meldeperiode: List<MeldeperiodeSammenligninger>,
) {
    data class MeldeperiodeSammenligninger(
        val periode: Periode,
        val dager: List<DagSammenligning>,
    )

    data class DagSammenligning(
        val dato: String,
        val status: ForrigeOgGjeldende<String>,
        val beløp: ForrigeOgGjeldende<Int>,
        val barnetillegg: ForrigeOgGjeldende<Int>,
        val prosent: ForrigeOgGjeldende<Int>,
    )

    /**
     * Holder på forrige og gjeldende verdi for en gitt type. Gjeldende verdi er enten nåværende tilstand
     * eller verdien som ble gjeldende etter en endring
     */
    data class ForrigeOgGjeldende<T>(
        val forrige: T?,
        val gjeldende: T,
    )
}

fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregning?,
    nyBeregning: MeldeperiodeBeregning,
): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
    // I de fleste cases er det ikke gjort noen korrigering, dermed er det bare nåtilstand som gjelder og det vil ikke finnes noen  "forrige" beregning
    if (forrigeBeregning == null) {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = nyBeregning.periode,
            dager = nyBeregning.dager.map {
                DagSammenligning(
                    dato = it.dato.format(norskUkedagOgDatoUtenÅrFormatter),
                    status = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.tilMeldekortDagStatus().toString(),
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
    require(forrigeBeregning.periode == nyBeregning.periode) { "Periodene må være like" }

    return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
        periode = forrigeBeregning.periode,
        dager = forrigeBeregning.dager
            .zip(nyBeregning.dager)
            .map { (forrige, gjeldende) -> sammenlign(forrige, gjeldende) },
    )
}

private fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregningDag,
    nyBeregning: MeldeperiodeBeregningDag,
): DagSammenligning {
    require(forrigeBeregning.dato == nyBeregning.dato) { "Datoene må være like" }

    return DagSammenligning(
        dato = forrigeBeregning.dato.format(norskUkedagOgDatoUtenÅrFormatter),
        status = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.tilMeldekortDagStatus().toString(),
            gjeldende = nyBeregning.tilMeldekortDagStatus().toString(),
        ),
        beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløp,
            gjeldende = nyBeregning.beløp,
        ),
        barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløpBarnetillegg,
            gjeldende = nyBeregning.beløpBarnetillegg,
        ),
        prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.prosent,
            gjeldende = nyBeregning.prosent,
        ),
    )
}
