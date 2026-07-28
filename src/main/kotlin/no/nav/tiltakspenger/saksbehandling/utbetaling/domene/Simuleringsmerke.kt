package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Det oppdragssystemet har å melde om én bestemt dag i beregningen.
 *
 * Merket sier at dagen er dekket av en postering, ikke hvor mye av posteringen som hører til dagen.
 * Skillet er poenget: **perioden er kildedata, mens en dagsandel ville vært oppdiktet.**
 * Har oppdrag sagt at justeringen gjelder 6.--10. januar, er det sant at mandag 6. er berørt -- men ingenting i kilden sier hvor mye av beløpet som hører til mandagen.
 *
 * Merket lar beregningsvisningen beholde dagsoppløsningen saksbehandler er vant til, uten at vi finner på tall.
 */
data class Simuleringsmerke(
    val type: Posteringstype,
    val periode: Periode,
    val klassekode: String,

    /**
     * Beløpet, men kun når posteringen dekker nøyaktig én dag.
     * Da er beløpet kildens eget og kan vises ved siden av dagen.
     *
     * Null når posteringen strekker seg over flere dager.
     * Da finnes det ingen dagsandel å oppgi, og visningen skal vise perioden i stedet.
     *
     * At vurderingen ligger her og ikke i frontend er med vilje.
     * «Kan dette tallet vises?» er nettopp spørsmålet vi har svart feil på før, og det hører hjemme ett sted.
     */
    val beløp: Int?,

    /**
     * Posteringsbeløpets fortegn, uavhengig av om beløpet kan vises.
     * Fortegnet er kildedata på posteringen og lar visningen skille et trekk fra en reversering av et tidligere trekk, også når posteringen dekker flere dager og [beløp] derfor er null.
     */
    val erNegativt: Boolean,
) {
    val erJustering: Boolean = klassekode == OppsummeringGenerator.KLASSEKODE_JUSTERING
}

fun Postering.tilSimuleringsmerke(): Simuleringsmerke = Simuleringsmerke(
    type = this.type,
    periode = this.periode,
    klassekode = this.klassekode,
    beløp = if (this.periode.fraOgMed == this.periode.tilOgMed) this.beløp else null,
    erNegativt = this.beløp < 0,
)
