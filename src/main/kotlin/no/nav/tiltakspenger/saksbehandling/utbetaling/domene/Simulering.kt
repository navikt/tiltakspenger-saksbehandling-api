package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * En dry-run av en utbetaling.
 * For mer informasjon, se: https://helved-docs.intern.dev.nav.no/v2/doc/simulering og https://confluence.adeo.no/display/OKSY/Returdata+fra+Oppdragssystemet+til+fagrutinen
 */
sealed interface Simulering {
    val simuleringstidspunkt: LocalDateTime

    fun hentDag(dato: LocalDate): Simuleringsdag?

    val harJustering: Boolean get() = this is Endring && harJustering
    val harFeilutbetaling: Boolean get() = this is Endring && harFeilutbetaling

    data class Endring(
        val simuleringPerMeldeperiode: NonEmptyList<SimuleringForMeldeperiode>,
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        override val simuleringstidspunkt: LocalDateTime,
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
        val totalMotpostering: Int by lazy { simuleringPerMeldeperiode.sumOf { it.totalMotpostering } }
        val totalJustering: Int by lazy { simuleringPerMeldeperiode.sumOf { it.simuleringsdager.sumOf { dag -> dag.totalJustering } } }
        val totalTrekk: Int by lazy { simuleringPerMeldeperiode.sumOf { it.totalTrekk } }

        override val harJustering: Boolean by lazy {
            simuleringPerMeldeperiode.any { it.harJustering }
        }

        override val harFeilutbetaling: Boolean by lazy {
            totalFeilutbetaling != 0
        }

        override fun hentDag(dato: LocalDate): Simuleringsdag? {
            return simuleringPerMeldeperiode
                .flatMap { it.simuleringsdager }
                .singleOrNullOrThrow {
                    it.dato == dato
                }
        }

        init {
            simuleringPerMeldeperiode.zipWithNext { a, b ->
                require(a.meldeperiode.periode.erFør(b.meldeperiode.periode)) {
                    "Simuleringene må være i rekkefølge. ${a.meldeperiode.periode} er før ${b.meldeperiode.periode}"
                }
            }
        }
    }

    data class IngenEndring(override val simuleringstidspunkt: LocalDateTime) : Simulering {
        override fun hentDag(dato: LocalDate) = null
    }
}

data class SimuleringForMeldeperiode(
    val meldeperiode: Meldeperiode,
    val simuleringsdager: NonEmptyList<Simuleringsdag>,
) {
    val tidligereUtbetalt: Int = simuleringsdager.sumOf { it.tidligereUtbetalt }
    val nyUtbetaling: Int = simuleringsdager.sumOf { it.nyUtbetaling }
    val totalEtterbetaling: Int = simuleringsdager.sumOf { it.totalEtterbetaling }
    val totalFeilutbetaling: Int = simuleringsdager.sumOf { it.totalFeilutbetaling }
    val totalMotpostering: Int = simuleringsdager.sumOf { it.totalMotpostering }
    val totalTrekk: Int = simuleringsdager.sumOf { it.totalTrekk }

    val harJustering: Boolean by lazy {
        simuleringsdager.any { it.harJustering }
    }
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

    /** Denne skal være lik negativ total feilutbetaling */
    val totalMotpostering: Int,

    /** F.eks. trekk fra namsmannen. Kommentar jah: Usikker på om denne vil vise omposteringer eller om det kun er justering som tar for seg det. */
    val totalTrekk: Int,
    /** Hvis denne dagen er negativt justert (typisk ompostert til en annen dag) */
    val totalJustering: Int,
    val harJustering: Boolean,

    /** Detaljene som ligger bak oppsummeringen. */
    val posteringsdag: PosteringerForDag,
) {
    @Suppress("unused")
    val harFeilutbetaling: Boolean by lazy { totalFeilutbetaling > 0 }

    @Suppress("unused")
    val harEtterbetaling: Boolean by lazy { totalEtterbetaling > 0 }

    @Suppress("unused")
    val harTrekk: Boolean by lazy { totalTrekk > 0 }
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
    // Kommentar jah: Vi kan beholde den som en String enn så lenge. Fyll inn javadoc etterhvert som vi oppdager de. Se også tilleggstønader: https://github.com/navikt/tilleggsstonader-sak/blob/main/src/main/kotlin/no/nav/tilleggsstonader/sak/utbetaling/simulering/kontrakt/SimuleringResponseDto.kt#L42
    // Eksempel: TILTAKSPENGER
    val fagområde: String,
    val beløp: Int,
    val type: Posteringstype,
    // Kommentar jah: Fyll ut eksempler etterhvert som vi oppdager de. Denne vil nok gjenspeile det vi sender inn i simuleringen, i hvert fall for de linjene som angår oss.
    // Eksempel: TPTPATT, TPTPGRAMO, TPBTGRAMO,KL_KODE_FEIL_ARBYT,KL_KODE_JUST_ARBYT,TBMOTOBS,TPBTAAGR,TPBTAF,TPBTOPPFAGR,TPTPAAG,TPTPAFT,TPTPOPPFAG
    val klassekode: String,
)

/**
 * Ved førstegangsutbetaling vil man i utgangspunktet kun få en postering av typen YTELSE, men det finnes mange unntak.
 * Kopiert fra https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDomain.kt#L44
 */
enum class Posteringstype {
    YTELSE,
    FEILUTBETALING,
    FORSKUDSSKATT,
    JUSTERING,
    TREKK,
    MOTPOSTERING,
}

fun Simulering?.erLik(other: Simulering?): Boolean = this.finnUlikheter(other).isEmpty()

/**
 * Finner ulikhetene mellom simuleringen fra beregningen og kontrollsimuleringen.
 * Returnerer en beskrivelse per ulikhet, eller en tom liste dersom simuleringene er like.
 * I beskrivelsene er `beregnet` simuleringen saksbehandler/beslutter så på behandlingen, og `kontroll` er kontrollsimuleringen som kjøres rett før iverksetting.
 * Sammenligner ikke enkeltposteringer, kun totalbeløpene for hver dag.
 */
fun Simulering?.finnUlikheter(kontrollsimulering: Simulering?): List<String> {
    if (this == null && kontrollsimulering == null) {
        return emptyList()
    }

    if (this is Simulering.IngenEndring && kontrollsimulering is Simulering.IngenEndring) {
        return emptyList()
    }

    if (this is Simulering.Endring && kontrollsimulering is Simulering.Endring) {
        if (this.simuleringPerMeldeperiode.size != kontrollsimulering.simuleringPerMeldeperiode.size) {
            return listOf("Ulikt antall meldeperioder: beregnet=${this.simuleringPerMeldeperiode.size}, kontroll=${kontrollsimulering.simuleringPerMeldeperiode.size}")
        }

        return this.simuleringPerMeldeperiode.toList().zip(kontrollsimulering.simuleringPerMeldeperiode).flatMap { (beregnet, kontroll) ->
            beregnet.finnUlikheter(kontroll)
        }
    }

    return listOf("Ulike simuleringstyper: beregnet=${this.beskriv()}, kontroll=${kontrollsimulering.beskriv()}")
}

private fun Simulering?.beskriv(): String {
    return when (this) {
        null -> "mangler"
        is Simulering.IngenEndring -> "ingen endring"
        is Simulering.Endring -> "endring (totalPeriode=$totalPeriode, tidligereUtbetalt=$tidligereUtbetalt, nyUtbetaling=$nyUtbetaling, totalEtterbetaling=$totalEtterbetaling, totalFeilutbetaling=$totalFeilutbetaling, totalJustering=$totalJustering, totalTrekk=$totalTrekk)"
    }
}

private fun SimuleringForMeldeperiode.finnUlikheter(kontroll: SimuleringForMeldeperiode): List<String> {
    if (this.meldeperiode.id != kontroll.meldeperiode.id) {
        return listOf("Ulike meldeperioder: beregnet=${this.meldeperiode.id}, kontroll=${kontroll.meldeperiode.id}")
    }

    if (this.simuleringsdager.size != kontroll.simuleringsdager.size) {
        return listOf("Ulikt antall simuleringsdager for meldeperiode ${this.meldeperiode.id}: beregnet=${this.simuleringsdager.size}, kontroll=${kontroll.simuleringsdager.size}")
    }

    return this.simuleringsdager.toList().zip(kontroll.simuleringsdager)
        .flatMap { (beregnetDag, kontrolldag) -> beregnetDag.finnUlikheter(kontrolldag) }
        .map { "Meldeperiode ${this.meldeperiode.id} $it" }
}

// Sjekker ikke om posteringene er like, kun totalbeløpene for hver dag
private fun Simuleringsdag.finnUlikheter(kontrolldag: Simuleringsdag): List<String> {
    if (!this.dato.isEqual(kontrolldag.dato)) {
        return listOf("har ulike datoer: beregnet=${this.dato}, kontroll=${kontrolldag.dato}")
    }

    val ulikeFelter = listOf(
        Triple("tidligereUtbetalt", tidligereUtbetalt, kontrolldag.tidligereUtbetalt),
        Triple("nyUtbetaling", nyUtbetaling, kontrolldag.nyUtbetaling),
        Triple("totalEtterbetaling", totalEtterbetaling, kontrolldag.totalEtterbetaling),
        Triple("totalFeilutbetaling", totalFeilutbetaling, kontrolldag.totalFeilutbetaling),
        Triple("totalMotpostering", totalMotpostering, kontrolldag.totalMotpostering),
        Triple("totalTrekk", totalTrekk, kontrolldag.totalTrekk),
        Triple("totalJustering", totalJustering, kontrolldag.totalJustering),
    ).mapNotNull { (felt, beregnet, kontroll) ->
        if (beregnet != kontroll) "$felt $beregnet->$kontroll" else null
    }

    return if (ulikeFelter.isEmpty()) {
        emptyList()
    } else {
        listOf("$dato (beregnet->kontroll): ${ulikeFelter.joinToString(", ")}")
    }
}
