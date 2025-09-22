package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import java.time.LocalDate

/**
 * En dry-run av en utbetaling.
 * For mer informasjon, se: https://helved-docs.intern.dev.nav.no/v2/doc/simulering og https://confluence.adeo.no/display/OKSY/Returdata+fra+Oppdragssystemet+til+fagrutinen
 */
sealed interface Simulering {

    fun hentDag(dato: LocalDate): Simuleringsdag?

    /**
     * @param eksterntTotalbeløp kommer fra Økonomisystemet, og er summen av alle posteringer i simuleringen. Ukjent hva slags algoritme som brukes. Ved simuleringer på tvers av meldeperioder, er ikke dette så relevant.
     * @param eksternDatoBeregnet kommer fra Økonomisystemet, og er datoen simuleringen ble beregnet.
     */
    data class Endring(
        val simuleringPerMeldeperiode: NonEmptyList<SimuleringForMeldeperiode>,
        val eksternDatoBeregnet: LocalDate,
        val eksterntTotalbeløp: Int,
    ) : Simulering {
        val meldeperioder: NonEmptyList<Meldeperiode> by lazy { simuleringPerMeldeperiode.map { it.meldeperiode } }
        val perioder: NonEmptyList<Periode> by lazy { meldeperioder.map { it.periode } }
        val totalPeriode: Periode by lazy {
            Periode(
                fraOgMed = perioder.minOf { it.fraOgMed },
                tilOgMed = perioder.maxOf { it.tilOgMed },
            )
        }
        val ordinær: Simuleringsbeløp? by lazy {
            if (simuleringPerMeldeperiode.all { it.ordinær == null }) {
                null
            } else {
                Simuleringsbeløp(
                    tidligereUtbetalt = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.tidligereUtbetalt }.sum(),
                    nyUtbetaling = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.nyUtbetaling }.sum(),
                    totalEtterbetaling = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.totalEtterbetaling }.sum(),
                    totalFeilutbetaling = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.totalFeilutbetaling }
                        .sum(),
                    totalTrekk = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.totalTrekk }.sum(),
                    totalJustering = simuleringPerMeldeperiode.mapNotNull { it.ordinær?.totalJustering }.sum(),
                )
            }
        }
        val barnetillegg: Simuleringsbeløp? by lazy {
            if (simuleringPerMeldeperiode.all { it.barnetillegg == null }) {
                null
            } else {
                Simuleringsbeløp(
                    tidligereUtbetalt = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.tidligereUtbetalt }
                        .sum(),
                    nyUtbetaling = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.nyUtbetaling }.sum(),
                    totalEtterbetaling = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.totalEtterbetaling }
                        .sum(),
                    totalFeilutbetaling = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.totalFeilutbetaling }
                        .sum(),
                    totalTrekk = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.totalTrekk }.sum(),
                    totalJustering = simuleringPerMeldeperiode.mapNotNull { it.barnetillegg?.totalJustering }.sum(),
                )
            }
        }

        val totalbeløp: Simuleringsbeløp? by lazy {
            if (ordinær == null && barnetillegg == null) return@lazy null
            Simuleringsbeløp(
                tidligereUtbetalt = (ordinær?.tidligereUtbetalt ?: 0) + (barnetillegg?.tidligereUtbetalt ?: 0),
                nyUtbetaling = (ordinær?.nyUtbetaling ?: 0) + (barnetillegg?.nyUtbetaling ?: 0),
                totalEtterbetaling = (ordinær?.totalEtterbetaling ?: 0) + (barnetillegg?.totalEtterbetaling ?: 0),
                totalFeilutbetaling = (ordinær?.totalFeilutbetaling ?: 0) + (barnetillegg?.totalFeilutbetaling ?: 0),
                totalTrekk = (ordinær?.totalTrekk ?: 0) + (barnetillegg?.totalTrekk ?: 0),
                totalJustering = (ordinær?.totalJustering ?: 0) + (barnetillegg?.totalJustering ?: 0),
            )
        }

        override fun hentDag(dato: LocalDate): Simuleringsdag? {
            return simuleringPerMeldeperiode
                .flatMap { it.simuleringsdager }
                .singleOrNullOrThrow {
                    @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
                    it.dato == dato
                }
        }

        @Suppress("unused")
        val harTidligereUtbetaling: Boolean by lazy { barnetillegg?.harTidligereUtbetaling == true || ordinær?.harTidligereUtbetaling == true }

        @Suppress("unused")
        val harFeilutbetaling: Boolean by lazy { barnetillegg?.harFeilutbetaling == true || ordinær?.harFeilutbetaling == true }

        @Suppress("unused")
        val harEtterbetaling: Boolean by lazy { barnetillegg?.harEtterbetaling == true || ordinær?.harEtterbetaling == true }

        @Suppress("unused")
        val harTrekk: Boolean by lazy { barnetillegg?.harTrekk == true || ordinær?.harTrekk == true }

        @Suppress("unused")
        val harJustering: Boolean by lazy { barnetillegg?.harJustering == true || ordinær?.harJustering == true }

        init {
            simuleringPerMeldeperiode.zipWithNext { a, b ->
                require(a.meldeperiode.periode.erFør(b.meldeperiode.periode)) {
                    "Simuleringene må være i rekkefølge. ${a.meldeperiode.periode} er før ${b.meldeperiode.periode}"
                }
            }
        }
    }

    data object IngenEndring : Simulering {
        override fun hentDag(dato: LocalDate) = null
    }
}

data class SimuleringForMeldeperiode(
    val meldeperiode: Meldeperiode,
    val simuleringsdager: NonEmptyList<Simuleringsdag>,
) {
    val ordinær: Simuleringsbeløp? by lazy {
        if (simuleringsdager.all { it.ordinær == null }) {
            null
        } else {
            Simuleringsbeløp(
                tidligereUtbetalt = simuleringsdager.mapNotNull { it.ordinær?.tidligereUtbetalt }.sum(),
                nyUtbetaling = simuleringsdager.mapNotNull { it.ordinær?.nyUtbetaling }.sum(),
                totalEtterbetaling = simuleringsdager.mapNotNull { it.ordinær?.totalEtterbetaling }.sum(),
                totalFeilutbetaling = simuleringsdager.mapNotNull { it.ordinær?.totalFeilutbetaling }.sum(),
                totalTrekk = simuleringsdager.mapNotNull { it.ordinær?.totalTrekk }.sum(),
                totalJustering = simuleringsdager.mapNotNull { it.ordinær?.totalJustering }.sum(),
            )
        }
    }
    val barnetillegg: Simuleringsbeløp? by lazy {
        if (simuleringsdager.all { it.barnetillegg == null }) {
            null
        } else {
            Simuleringsbeløp(
                tidligereUtbetalt = simuleringsdager.mapNotNull { it.barnetillegg?.tidligereUtbetalt }.sum(),
                nyUtbetaling = simuleringsdager.mapNotNull { it.barnetillegg?.nyUtbetaling }.sum(),
                totalEtterbetaling = simuleringsdager.mapNotNull { it.barnetillegg?.totalEtterbetaling }.sum(),
                totalFeilutbetaling = simuleringsdager.mapNotNull { it.barnetillegg?.totalFeilutbetaling }.sum(),
                totalTrekk = simuleringsdager.mapNotNull { it.barnetillegg?.totalTrekk }.sum(),
                totalJustering = simuleringsdager.mapNotNull { it.barnetillegg?.totalJustering }.sum(),
            )
        }
    }

    val totalbeløp: Simuleringsbeløp? by lazy {
        if (ordinær == null && barnetillegg == null) return@lazy null
        Simuleringsbeløp(
            tidligereUtbetalt = (ordinær?.tidligereUtbetalt ?: 0) + (barnetillegg?.tidligereUtbetalt ?: 0),
            nyUtbetaling = (ordinær?.nyUtbetaling ?: 0) + (barnetillegg?.nyUtbetaling ?: 0),
            totalEtterbetaling = (ordinær?.totalEtterbetaling ?: 0) + (barnetillegg?.totalEtterbetaling ?: 0),
            totalFeilutbetaling = (ordinær?.totalFeilutbetaling ?: 0) + (barnetillegg?.totalFeilutbetaling ?: 0),
            totalTrekk = (ordinær?.totalTrekk ?: 0) + (barnetillegg?.totalTrekk ?: 0),
            totalJustering = (ordinær?.totalJustering ?: 0) + (barnetillegg?.totalJustering ?: 0),
        )
    }

    @Suppress("unused")
    val harTidligereUtbetaling: Boolean by lazy { barnetillegg?.harTidligereUtbetaling == true || ordinær?.harTidligereUtbetaling == true }

    @Suppress("unused")
    val harFeilutbetaling: Boolean by lazy { barnetillegg?.harFeilutbetaling == true || ordinær?.harFeilutbetaling == true }

    @Suppress("unused")
    val harEtterbetaling: Boolean by lazy { barnetillegg?.harEtterbetaling == true || ordinær?.harEtterbetaling == true }

    @Suppress("unused")
    val harTrekk: Boolean by lazy { barnetillegg?.harTrekk == true || ordinær?.harTrekk == true }

    @Suppress("unused")
    val harJustering: Boolean by lazy { barnetillegg?.harJustering == true || ordinær?.harJustering == true }
}

/**
 * Må enten ha ordinær eller barnetillegg, eller begge.
 */
data class Simuleringsdag(
    val dato: LocalDate,
    val ordinær: Simuleringsbeløp?,
    val barnetillegg: Simuleringsbeløp?,

    /** Detaljene som ligger bak oppsummeringen. */
    val posteringsdag: PosteringerForDag,
) {
    init {
        require((ordinær != null) || (barnetillegg != null)) {
            "Må ha enten ordinær eller barnetillegg, eller begge."
        }
    }

    /**
     * Summen av ordinær og barnetillegg, eller null hvis begge er null.
     */
    val totalbeløp: Simuleringsbeløp? by lazy {
        if (ordinær == null && barnetillegg == null) return@lazy null
        Simuleringsbeløp(
            tidligereUtbetalt = (ordinær?.tidligereUtbetalt ?: 0) + (barnetillegg?.tidligereUtbetalt ?: 0),
            nyUtbetaling = (ordinær?.nyUtbetaling ?: 0) + (barnetillegg?.nyUtbetaling ?: 0),
            totalEtterbetaling = (ordinær?.totalEtterbetaling ?: 0) + (barnetillegg?.totalEtterbetaling ?: 0),
            totalFeilutbetaling = (ordinær?.totalFeilutbetaling ?: 0) + (barnetillegg?.totalFeilutbetaling ?: 0),
            totalTrekk = (ordinær?.totalTrekk ?: 0) + (barnetillegg?.totalTrekk ?: 0),
            totalJustering = (ordinær?.totalJustering ?: 0) + (barnetillegg?.totalJustering ?: 0),
        )
    }

    @Suppress("unused")
    val harTidligereUtbetaling: Boolean by lazy { barnetillegg?.harTidligereUtbetaling == true || ordinær?.harTidligereUtbetaling == true }

    @Suppress("unused")
    val harFeilutbetaling: Boolean by lazy { barnetillegg?.harFeilutbetaling == true || ordinær?.harFeilutbetaling == true }

    @Suppress("unused")
    val harEtterbetaling: Boolean by lazy { barnetillegg?.harEtterbetaling == true || ordinær?.harEtterbetaling == true }

    @Suppress("unused")
    val harTrekk: Boolean by lazy { barnetillegg?.harTrekk == true || ordinær?.harTrekk == true }

    @Suppress("unused")
    val harJustering: Boolean by lazy { barnetillegg?.harJustering == true || ordinær?.harJustering == true }
}

data class Simuleringsbeløp(
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

    /** F.eks. trekk fra namsmannen. Kommentar jah: Usikker på om denne vil vise omposteringer eller om det kun er justering som tar for seg det. */
    val totalTrekk: Int,

    /** Hvis denne dagen er negativt justert (typisk ompostert til en annen dag) */
    val totalJustering: Int,
) {
    @Suppress("unused")
    val harTidligereUtbetaling: Boolean by lazy { tidligereUtbetalt != 0 }

    @Suppress("unused")
    val harFeilutbetaling: Boolean by lazy { totalFeilutbetaling != 0 }

    @Suppress("unused")
    val harEtterbetaling: Boolean by lazy { totalEtterbetaling != 0 }

    @Suppress("unused")
    val harTrekk: Boolean by lazy { totalTrekk != 0 }

    @Suppress("unused")
    val harJustering: Boolean by lazy { totalJustering != 0 }
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
