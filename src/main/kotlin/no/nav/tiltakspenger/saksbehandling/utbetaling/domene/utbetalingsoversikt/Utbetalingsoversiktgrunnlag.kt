package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Det vi har behandlingsgrunnlag for å lagre om en persons utbetalinger fra økonomisystemet.
 * En ytelse beholdes bare når typen er en tiltakspenger samordnes med, perioden overlapper sakens perioder, og personen er rettighetshaver.
 * Oppslaget mot tjenesten er bredere enn dette, og det som faller utenfor, tas bort før lagring og telles per årsak.
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

    /** Finner årsaken til at en ytelse faller utenfor grunnlaget, eller null når ytelsen skal beholdes. */
    fun avgrensningsårsak(ytelsestype: String?, periode: Periode, rettighetshaverIdent: String?): Avgrensningsårsak? = when {
        ytelsestype == null -> Avgrensningsårsak.UTEN_YTELSESTYPE
        ytelsestype !in ytelsestyper -> Avgrensningsårsak.ANNEN_YTELSESTYPE
        !periode.overlapperMed(perioder) -> Avgrensningsårsak.UTENFOR_PERIODENE
        rettighetshaverIdent != fnr.verdi -> Avgrensningsårsak.ANNEN_RETTIGHETSHAVER
        else -> null
    }

    companion object {
        /** Økes når reglene over endres, så lagrede oppslag kan leses mot regelen de ble avgrenset med. */
        const val REGELVERSJON = 1

        /** Beskrivelsene tjenesten bruker for ytelsene vi samordner med; tjenesten sender ikke koder. */
        val ytelsestyper: Set<String> = Ytelsetype.entries.filter { it != Ytelsetype.UKJENT }.map { it.tekstverdi }.toSet()

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

/** Årsaken til at en mottatt ytelse tas bort før lagring. */
enum class Avgrensningsårsak {
    UTEN_YTELSESTYPE,
    ANNEN_YTELSESTYPE,
    UTENFOR_PERIODENE,
    ANNEN_RETTIGHETSHAVER,
}
