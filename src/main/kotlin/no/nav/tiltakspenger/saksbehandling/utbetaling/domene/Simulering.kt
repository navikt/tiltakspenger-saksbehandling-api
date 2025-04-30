package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

/**
 * En dry-run av en utbetaling.
 * For mer informasjon, se: https://helved-docs.intern.dev.nav.no/v2/doc/simulering og https://confluence.adeo.no/display/OKSY/Returdata+fra+Oppdragssystemet+til+fagrutinen
 */
data class Simulering(
    val oppsummeringForPerioder: List<OppsummeringForPeriode>,
    val detaljer: Detaljer,
) {
    data class OppsummeringForPeriode(
        val periode: Periode,

        /** Totalbeløpet som er utbetalt til bruker på saken tidligere, for perioden oppsummeringen gjelder for. */
        val tidligereUtbetalt: Int,

        /**
         * Det nye gjeldende totalbeløpet som skal utbetales til bruker for perioden.
         * Dette er ikke nødvendigvis det som faktisk blir brutto utbetalt – hvis simuleringen gjelder en revurdering, må det nye beløpet ses i sammenheng med det tidligere utbetalte beløpet.
         * Er tidligere utbetalt 800 kr og nytt beløp 1000 kr, vil brukeren få utbetalt 200 kr.
         */
        val nyUtbetaling: Int,

        /**
         * Simuleringen viser en etterbetaling dersom bruker får en økning i utbetalingen for en periode tilbake i tid.
         * Det kan gjelde både en helt ny utbetaling og en økning i allerede utbetalt beløp.
         * Dersom perioden oppsummeringen gjelder for er frem i tid, vil etterbetalingen alltid være 0.
         * Etterbetalingen kan aldri være negativ – dersom en periode har en reduksjon i tidligere utbetalt beløp, vil etterbetalingen være 0.
         * Selv om brukeren får en ny utbetaling eller en økning i beløp, er det ikke alltid slik at etterbetalingen er differansen på det nye beløpet og tidligere utbetalt.
         * OS kan i noen tilfeller bruke en økning i en periode for å dekke opp for en reduksjon i en annen periode
         * Dette gjelder dersom økningen og reduksjonen skjer innenfor samme måned, eller når økningen skjer den påfølgende måneden etter reduksjonen.
         */
        val totalEtterbetaling: Int,

        /**
         * Simuleringen vil ha en positiv feilutbetaling dersom Utsjekk mottar eksplisitte posteringer for feilutbetaling fra OS.
         * Dette feltet er summen av disse posteringene. Vedtaksløsningene kan anta at det vil komme et kravgrunnlag for tilbakekreving hvis simuleringen viser en positiv feilutbetaling.
         * Feilutbetalingen vil alltid være ikke-negativ.
         */
        val totalFeilutbetaling: Int,

        // TODO jah: Følg opp helved og spør om vi kan få med trekk.
    )

    data class Detaljer(
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        val perioder: List<Simuleringsperiode>,
    ) {
        data class Simuleringsperiode(
            val periode: Periode,
            val posteringer: List<Postering>,
        ) {
            data class Postering(
                // TODO jah: Vi kan beholde den som en String enn så lenge. Fyll inn javadoc etterhvert som vi oppdager de. Se også tilleggstønader: https://github.com/navikt/tilleggsstonader-sak/blob/main/src/main/kotlin/no/nav/tilleggsstonader/sak/utbetaling/simulering/kontrakt/SimuleringResponseDto.kt#L42
                val fagområde: String,
                val periode: Periode,
                val beløp: Int,
                // TODO jah: Beholder den som String enn så lenge. Se også tilleggstønader: https://github.com/navikt/tilleggsstonader-sak/blob/main/src/main/kotlin/no/nav/tilleggsstonader/sak/utbetaling/simulering/kontrakt/PosteringType.kt
                /** Kan blant annet være: YTEL, FEIL, SKAT, JUST, TREK, MOTP */
                val type: String,
                // TODO jah: Fyll ut eksempler etterhvert som vi oppdager de. Denne vil nok gjenspeile det vi sender inn i simuleringen, i hvert fall for de linjene som angår oss.
                val klassekode: String,
            )
        }
    }
}
