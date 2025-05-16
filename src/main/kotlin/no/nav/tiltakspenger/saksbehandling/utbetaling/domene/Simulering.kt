package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import java.time.LocalDate

/**
 * En dry-run av en utbetaling.
 * For mer informasjon, se: https://helved-docs.intern.dev.nav.no/v2/doc/simulering og https://confluence.adeo.no/display/OKSY/Returdata+fra+Oppdragssystemet+til+fagrutinen
 */
sealed interface Simulering {
    data class Endring(
        val simuleringPerMeldeperiode: NonEmptyList<SimuleringForMeldeperiode>,
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
    ) : Simulering {
        val meldeperioder: NonEmptyList<Meldeperiode> by lazy { simuleringPerMeldeperiode.map { it.meldeperiode } }
        val perioder: NonEmptyList<Periode> by lazy { meldeperioder.map { it.periode } }
        val totalPeriode: Periode by lazy {
            Periode(
                fraOgMed = perioder.minOf { it.fraOgMed },
                tilOgMed = perioder.maxOf { it.tilOgMed },
            )
        }
        val tidligereUtbetalt: Int by lazy { simuleringPerMeldeperiode.sumOf { it.tidligereUtbetalt } }
        val nyUtbetaling: Int by lazy { simuleringPerMeldeperiode.sumOf { it.nyUtbetaling } }
        val totalEtterbetaling: Int by lazy { simuleringPerMeldeperiode.sumOf { it.totalEtterbetaling } }
        val totalFeilutbetaling: Int by lazy { simuleringPerMeldeperiode.sumOf { it.totalFeilutbetaling } }
        // TODO jah: Legg til trekk og motpostering

        init {
            simuleringPerMeldeperiode.zipWithNext { a, b ->
                require(a.meldeperiode.periode.erFør(b.meldeperiode.periode)) {
                    "Simuleringene må være i rekkefølge. ${a.meldeperiode.periode} er før ${b.meldeperiode.periode}"
                }
            }
        }
    }

    data object IngenEndring : Simulering
}

data class SimuleringForMeldeperiode(
    val meldeperiode: Meldeperiode,
    val simuleringsdager: NonEmptyList<Simuleringsdag>,
) {
    val tidligereUtbetalt: Int = simuleringsdager.sumOf { it.tidligereUtbetalt }
    val nyUtbetaling: Int = simuleringsdager.sumOf { it.nyUtbetaling }
    val totalEtterbetaling: Int = simuleringsdager.sumOf { it.totalEtterbetaling }
    val totalFeilutbetaling: Int = simuleringsdager.sumOf { it.totalFeilutbetaling }
    // TODO jah: Legg til trekk og motpostering
}

data class Simuleringsdag(
    val dato: LocalDate,

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
    // TODO jah: Legg til trekk og motpostering
    /**
     * Detaljene som ligger bak oppsummeringen.
     */
    val posteringsdag: PosteringerForDag,
) {
    val harFeilutbetaling: Boolean by lazy { totalFeilutbetaling > 0 }
    val harEtterbetaling: Boolean by lazy { totalEtterbetaling > 0 }
}

/**
 * OS leverer en sammenhengende periode med en eller flere posteringer, som vi splitter opp i dager.
 */
data class PosteringerForDag(
    val dato: LocalDate,
    val posteringer: NonEmptyList<PosteringForDag>,
)

/**
 * Slik vi fikk detaljene fra helved, men splittet opp i dager innenfor en meldeperiode.
 */
data class PosteringForDag(
    val dato: LocalDate,
    // TODO jah: Vi kan beholde den som en String enn så lenge. Fyll inn javadoc etterhvert som vi oppdager de. Se også tilleggstønader: https://github.com/navikt/tilleggsstonader-sak/blob/main/src/main/kotlin/no/nav/tilleggsstonader/sak/utbetaling/simulering/kontrakt/SimuleringResponseDto.kt#L42
    // Eksempel: TILTAKSPENGER
    val fagområde: String,
    val beløp: Int,
    val type: Posteringstype,
    // TODO jah: Fyll ut eksempler etterhvert som vi oppdager de. Denne vil nok gjenspeile det vi sender inn i simuleringen, i hvert fall for de linjene som angår oss.
    // Eksempel: TPTPATT
    val klassekode: String,
)

/**
 * Ved førstegangsutbetaling vil man i utgangspunktet kun få en postering av typen YTELSE, men det finnes mange unntak.
 * Kopiert fra https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDomain.kt#L44
 */
@Suppress("unused")
enum class Posteringstype {
    YTELSE,
    FEILUTBETALING,
    FORSKUDSSKATT,
    JUSTERING,
    TREKK,
    MOTPOSTERING,
}
