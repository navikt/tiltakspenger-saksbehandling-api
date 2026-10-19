package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.time.DayOfWeek

/**
 * Det vi har behandlingsgrunnlag for å lagre om en persons utbetalinger fra økonomisystemet.
 * En ytelse beholdes bare når typen er en tiltakspenger samordnes med, perioden overlapper sakens perioder, og personen er rettighetshaver.
 * Oppslaget mot tjenesten er bredere enn dette, og det som faller utenfor, tas bort før lagring og telles per årsak.
 * For tiltakspenger regnes en periode som starter i helgen, fra fredagen før, fordi vi utbetaler helgedagene på fredagen i samme uke.
 * Økonomisystemet deler ytelsene per kalendermåned, så den fredagen kan komme som en egen ytelse i måneden før.
 *
 * @param perioder Sakens behandlingsgrunnlagsperioder, sortert og slått sammen.
 */
data class Utbetalingsoversiktgrunnlag(
    val fnr: Fnr,
    val perioder: List<Periode>,
) {
    init {
        require(perioder.isNotEmpty()) { "Grunnlaget må ha minst én periode" }
    }

    private val perioderForTiltakspenger: List<Periode> = perioder.map { it.utvidetMedFredagFørHelg() }

    /**
     * Finner årsaken til at en ytelse faller utenfor grunnlaget, eller null når ytelsen skal beholdes.
     *
     * @param ytelsestype null når ytelsestype mangler, og [Ytelsetype.UKJENT] når vi ikke samordner med den.
     * @param rettighetshaver null når rettighetshaveren mangler eller ikke har et fødselsnummer.
     */
    fun avgrensningsårsak(ytelsestype: Ytelsetype?, periode: Periode, rettighetshaver: Fnr?): Avgrensningsårsak? = when {
        ytelsestype == null -> Avgrensningsårsak.UTEN_YTELSESTYPE
        ytelsestype == Ytelsetype.UKJENT -> Avgrensningsårsak.ANNEN_YTELSESTYPE
        !periode.overlapperMed(perioderFor(ytelsestype)) -> Avgrensningsårsak.UTENFOR_PERIODENE
        rettighetshaver != fnr -> Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
        else -> null
    }

    private fun perioderFor(ytelsestype: Ytelsetype): List<Periode> =
        if (ytelsestype == Ytelsetype.TILTAKSPENGER) perioderForTiltakspenger else perioder

    companion object {
        /**
         * Økes når reglene over endres etter at jobben er satt i produksjon, slik at lagrede oppslag kan leses mot regelen de ble avgrenset med.
         * Fram til da leses ingen lagrede oppslag, så versjonen står på 1.
         */
        const val REGELVERSJON = 1

        /**
         * Grunnlaget fra sakens behandlingsgrunnlagsperioder.
         * Forutsetter at saken har en søknad med tiltak eller et rammevedtak.
         */
        fun forSak(sak: Sak): Utbetalingsoversiktgrunnlag = Utbetalingsoversiktgrunnlag(
            fnr = sak.fnr,
            perioder = requireNotNull(sak.behandlingsgrunnlagsperioder) { "Sak ${sak.id} har ingen behandlingsgrunnlagsperioder" }.perioder,
        )
    }
}

/** Returnerer perioden utvidet med fredagen før dersom den starter i en helg, ellers uendret. */
private fun Periode.utvidetMedFredagFørHelg(): Periode = when (fraOgMed.dayOfWeek) {
    DayOfWeek.SATURDAY -> minusFraOgMed(1)
    DayOfWeek.SUNDAY -> minusFraOgMed(2)
    else -> this
}

/** Årsaken til at en mottatt ytelse tas bort før lagring. */
enum class Avgrensningsårsak {
    UTEN_YTELSESTYPE,
    ANNEN_YTELSESTYPE,
    UTENFOR_PERIODENE,
    ANNEN_RETTIGHETSHAVER,
}
