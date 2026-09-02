package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.abs

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
            simuleringPerMeldeperiode.any { it.harFeilutbetaling }
        }

        /**
         * Justeringene summert per kalendermåned på tvers av hele simuleringen, kun for månedene som ikke går opp i null.
         * Tom betyr at justeringene bare flytter beløp innenfor månedene -- for eksempel når oppdrag omfordeler forskuddstrekk, som beregnes per måned.
         */
        val ubalanserteJusteringsmåneder: Map<YearMonth, Int> by lazy {
            simuleringPerMeldeperiode
                .flatMap { it.posteringer }
                .filter { it.erJustering }
                .groupBy { YearMonth.from(it.periode.fraOgMed) }
                .mapValues { (_, justeringerForMåned) -> justeringerForMåned.sumOf { it.beløp } }
                .filterValues { it != 0 }
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

    /**
     * Posteringene slik oppdragssystemet leverte dem, med sine egne perioder.
     * Dette er kilden dagsverdiene i [simuleringsdager] er utledet fra.
     */
    val posteringer: NonEmptyList<Postering>,
) {
    val tidligereUtbetalt: Int = simuleringsdager.sumOf { it.tidligereUtbetalt }
    val nyUtbetaling: Int = simuleringsdager.sumOf { it.nyUtbetaling }
    val totalEtterbetaling: Int = simuleringsdager.sumOf { it.totalEtterbetaling }
    val totalFeilutbetaling: Int = simuleringsdager.sumOf { it.totalFeilutbetaling }
    val totalMotpostering: Int = simuleringsdager.sumOf { it.totalMotpostering }
    val totalTrekk: Int = simuleringsdager.sumOf { it.totalTrekk }

    /**
     * Fakta om hva simuleringen sier om denne meldeperioden.
     * Både vernet i valideringen og flagget saksbehandler får se leser herfra, slik at de ikke kan drive fra hverandre.
     */
    val flagg: Simuleringsflagg by lazy {
        Simuleringsflagg.fraPosteringer(
            posteringer = posteringer,
            harUbalansertJustering = harUbalansertJustering,
        )
    }

    /**
     * Gjenkjennes på posteringene, ikke på dagsverdiene.
     * For eldre lagrede rader er dagens `harJustering` utledet med en default som ikke alltid stemmer, mens klassekoden på posteringene ligger fast.
     */
    val harJustering: Boolean by lazy {
        posteringer.any { it.erJustering }
    }

    /** Se [Simuleringsflagg.harFeilutbetaling] for hvorfor bare positive posteringer teller. */
    val harFeilutbetaling: Boolean by lazy {
        flagg.harFeilutbetaling
    }

    /**
     * Tidligere utbetalt ytelse i meldeperioden reverseres.
     * Sammen med en ubalansert justering betyr det at selve ytelsen er motregnet mot en annen meldeperiode -- ikke at trekk er omfordelt.
     */
    val harReversertYtelse: Boolean by lazy {
        posteringer.any { it.type == Posteringstype.YTELSE && it.beløp < 0 }
    }

    /**
     * Summen av justeringsposteringene per kalendermåned, for månedene der summen ikke går opp i null.
     * Tom når justeringene balanserer.
     *
     * Oppdrag beregner per kalendermåned, så en justering som balanserer innenfor måneden er en omfordeling mellom dager vi kan leve med.
     * Går den ikke opp, er beløpet flyttet ut av meldeperioden eller måneden, og det har vi ikke hjemmel til.
     *
     * Summeres på posteringene, ikke på dagsverdiene.
     * En justering kan ha et beløp som ikke går opp i antall dager i perioden sin, og da ville avrundingen av dagsverdiene kunne gi en sum ulik null for et justeringssett som balanserer perfekt hos oppdragssystemet.
     */
    val ubalanserteJusteringsmåneder: Map<YearMonth, Int> by lazy {
        posteringer
            .filter { it.erJustering }
            .groupBy { YearMonth.from(it.periode.fraOgMed) }
            .mapValues { (_, justeringerForMåned) -> justeringerForMåned.sumOf { it.beløp } }
            .filterValues { it != 0 }
    }

    /** Se [ubalanserteJusteringsmåneder]. */
    val harUbalansertJustering: Boolean by lazy {
        harJustering && ubalanserteJusteringsmåneder.isNotEmpty()
    }
}

/**
 * En postering kan ikke strekke seg utover meldeperioden den hører til.
 *
 * Grensen er vår egen, ikke oppdragssystemets: `Beregning.tilUtbetalingerDTO` bygger utbetalingslinjene per meldeperiodekjede, og oppdrag arver de grensene i svaret.
 * Derfor er dette en invariant vi konstruerer, ikke en antakelse vi gjør om et system vi ikke styrer.
 * Brytes den, er dagsverdiene vi utleder oppdiktet og summene per meldeperiode gale uten at noe synes.
 *
 * Sjekken kjøres på begge byggeveiene: [OppsummeringGenerator] svarer med typet feil når svaret fra oppdragssystemet bryter den, og lesing fra databasen feiler høylytt på en lagret rad som gjør det.
 */
fun Postering.utenforMeldeperiodenFeil(meldeperiode: Meldeperiode): Simuleringsfeil.PosteringUtenforMeldeperiode? {
    if (meldeperiode.periode.inneholderHele(this.periode)) {
        return null
    }
    return Simuleringsfeil.PosteringUtenforMeldeperiode(
        meldeperiodeId = meldeperiode.id,
        meldeperiode = meldeperiode.periode,
        postering = this.periode,
        klassekode = this.klassekode,
        type = this.type,
    )
}

/** Se [utenforMeldeperiodenFeil]. */
fun SimuleringForMeldeperiode.finnPosteringUtenforMeldeperioden(): Simuleringsfeil.PosteringUtenforMeldeperiode? {
    return posteringer.firstNotNullOfOrNull { it.utenforMeldeperiodenFeil(meldeperiode) }
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
     * Dette feltet er summen av disse posteringene.
     * Vedtaksløsningene kan anta at det vil komme et kravgrunnlag for tilbakekreving hvis simuleringen viser en positiv feilutbetaling.
     * Feilutbetalingen vil alltid være ikke-negativ.
     */
    val totalFeilutbetaling: Int,

    /** Denne skal være lik negativ total feilutbetaling */
    val totalMotpostering: Int,

    /**
     * F.eks. trekk fra namsmannen.
     * Kommentar jah: Usikker på om denne vil vise omposteringer eller om det kun er justering som tar for seg det.
     */
    val totalTrekk: Int,
    /** Hvis denne dagen er negativt justert (typisk ompostert til en annen dag) */
    val totalJustering: Int,
    val harJustering: Boolean,
) {
    @Suppress("unused")
    val harFeilutbetaling: Boolean by lazy { totalFeilutbetaling > 0 }

    @Suppress("unused")
    val harEtterbetaling: Boolean by lazy { totalEtterbetaling > 0 }

    /**
     * Trekk kommer med begge fortegn, og de aller fleste er negative.
     * Derfor holder det ikke å sjekke om beløpet er positivt.
     */
    @Suppress("unused")
    val harTrekk: Boolean by lazy { totalTrekk != 0 }
}

/**
 * En postering slik oppdragssystemet leverte den, med sin egen periode.
 * Beløpet gjelder hele perioden under ett, ikke per dag.
 *
 * For ytelsesposteringer går beløpet opp i antall dager, fordi vi selv sender hele kroner per dag og OS slår sammen dager med samme dagsbeløp.
 * For motregning -- feilutbetaling, motpostering, justering og trekk -- gjør det ofte ikke det.
 * Da har OS balansert dager mot hverandre, beløpet svarer bare til deler av perioden det er stemplet med, og det finnes ingen dagsfordeling å gjenskape.
 * Derfor beholder vi perioden slik den kom, og utleder dagsverdier kun til visning.
 */
data class Postering(
    val periode: Periode,
    // Kommentar jah: Vi kan beholde den som en String enn så lenge.
    // Fyll inn javadoc etterhvert som vi oppdager de.
    // Se også tilleggstønader: https://github.com/navikt/tilleggsstonader-sak/blob/main/src/main/kotlin/no/nav/tilleggsstonader/sak/utbetaling/simulering/kontrakt/SimuleringResponseDto.kt#L42
    // Eksempel: TILTAKSPENGER
    val fagområde: String,
    val beløp: Int,
    val type: Posteringstype,
    // Kommentar jah: Fyll ut eksempler etterhvert som vi oppdager de.
    // Denne vil nok gjenspeile det vi sender inn i simuleringen, i hvert fall for de linjene som angår oss.
    // Eksempel: TPTPATT, TPTPGRAMO, TPBTGRAMO,KL_KODE_FEIL_ARBYT,KL_KODE_JUST_ARBYT,TBMOTOBS,TPBTAAGR,TPBTAF,TPBTOPPFAGR,TPTPAAG,TPTPAFT,TPTPOPPFAG
    val klassekode: String,
) {
    /**
     * Justeringer gjenkjennes på klassekoden alene, ikke på posteringstypen.
     * OS har sendt samme begrep både som `FEILUTBETALING` og som `JUSTERING` med denne klassekoden, mens klassekoden har ligget fast i alt materiale vi har sett.
     */
    val erJustering: Boolean = klassekode == OppsummeringGenerator.KLASSEKODE_JUSTERING

    /**
     * Fordeler beløpet på dagene i perioden uten å miste resten.
     * Går beløpet opp i antall dager, får hver dag like mye.
     * Gjør det ikke det, legges resten ut på de første dagene med én krone hver, slik at summen av dagene alltid er nøyaktig lik [beløp].
     *
     * Hvilke dager som får den ekstra krona er vilkårlig -- kilden sier ingenting om det.
     * Det er likevel trygt, fordi en postering alltid ligger innenfor én meldeperiode.
     * Valget kan derfor aldri flytte beløp mellom meldeperioder, og summen per meldeperiode blir den samme uansett.
     */
    fun beløpPerDag(): Map<LocalDate, Int> {
        val dager = periode.tilDager()
        val beløpPerDag = beløp / dager.size
        val rest = beløp % dager.size
        val ekstra = if (rest < 0) -1 else 1
        return dager.mapIndexed { index, dato ->
            dato to if (index < abs(rest)) beløpPerDag + ekstra else beløpPerDag
        }.toMap()
    }
}

/**
 * Ved førstegangsutbetaling vil man i utgangspunktet kun få en postering av typen YTELSE, men det finnes mange unntak.
 * Kopiert fra `domain.PosteringType`, som er enumen helved serialiserer inn i posteringene de sender oss.
 * Se [SimuleringModels.kt](https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringModels.kt).
 */
enum class Posteringstype {
    YTELSE,
    FEILUTBETALING,
    FORSKUDSSKATT,
    JUSTERING,
    TREKK,
    MOTPOSTERING,
}

fun Simulering?.erLik(other: Simulering?): Boolean = this.finnUlikheter(other, fraOgMed = null).isEmpty()

/**
 * Finner ulikhetene mellom simuleringen fra beregningen og kontrollsimuleringen.
 * Returnerer en beskrivelse per ulikhet, eller en tom liste dersom simuleringene er like.
 * I beskrivelsene er `beregnet` simuleringen saksbehandler/beslutter så på behandlingen, og `kontroll` er kontrollsimuleringen som kjøres rett før iverksetting.
 *
 * Sammenligner posteringene, som er kildedataene fra oppdragssystemet.
 * Dagsverdiene er utledet av posteringene til visning og kan ikke avvike uten at en postering avviker -- å sammenligne dem ville bare lagt avrundingsvalgene våre inn i sammenligningen.
 *
 * Eldre lagrede simuleringer har posteringene splittet opp per dag.
 * Mot en fersk kontrollsimulering gir det forskjell i form uten forskjell i innhold, og det fanges her med vilje: tallene beslutter så på skal være tallene som iverksettes, og saksbehandler løser det med «Oppdater simulering».
 *
 * [fraOgMed] fraOgMed dato for behandlingen som simuleringene er kjørt fra.
 * Tidligere meldeperioder enn dette er ikke relevante for behandlingen, og forkastes fra kontrollsimuleringen.
 * Obs: tidligere meldeperioder kan i noen tilfeller allikevel påvirke sammenligningen, dersom simuleringen i utgangspunktet ikke viste noen endringer.
 */
fun Simulering?.finnUlikheter(kontrollsimulering: Simulering?, fraOgMed: LocalDate?): List<String> {
    if (this == null && kontrollsimulering == null) {
        return emptyList()
    }

    if (this is Simulering.IngenEndring && kontrollsimulering is Simulering.IngenEndring) {
        return emptyList()
    }

    if (this is Simulering.Endring && kontrollsimulering is Simulering.Endring) {
        val kontrollsimuleringPerioder = if (fraOgMed != null) {
            kontrollsimulering.simuleringPerMeldeperiode.filter { it.meldeperiode.periode.tilOgMed >= fraOgMed }
        } else {
            kontrollsimulering.simuleringPerMeldeperiode
        }

        if (this.simuleringPerMeldeperiode.size != kontrollsimuleringPerioder.size) {
            return listOf("Ulikt antall meldeperioder: beregnet=${this.simuleringPerMeldeperiode.size}, kontroll=${kontrollsimuleringPerioder.size}")
        }

        return this.simuleringPerMeldeperiode.toList().zip(kontrollsimuleringPerioder).flatMap { (beregnet, kontroll) ->
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

    val kunIBeregnet = this.posteringer.utenom(kontroll.posteringer)
    val kunIKontroll = kontroll.posteringer.utenom(this.posteringer)

    if (kunIBeregnet.isEmpty() && kunIKontroll.isEmpty()) {
        return emptyList()
    }

    return listOf(
        buildString {
            append("Meldeperiode ${meldeperiode.id} har ulike posteringer.")
            if (kunIBeregnet.isNotEmpty()) {
                append(" Kun i beregnet: ${kunIBeregnet.joinToString { it.beskriv() }}.")
            }
            if (kunIKontroll.isNotEmpty()) {
                append(" Kun i kontroll: ${kunIKontroll.joinToString { it.beskriv() }}.")
            }
        },
    )
}

/**
 * Posteringene i denne listen som ikke har en make i [andre], der hver make bare kan brukes én gang.
 * Rekkefølgen posteringene kom i fra oppdragssystemet er ikke en del av innholdet, så den sammenlignes ikke.
 */
private fun List<Postering>.utenom(andre: List<Postering>): List<Postering> {
    val gjenstående = andre.toMutableList()
    return filter { !gjenstående.remove(it) }
}

private fun Postering.beskriv(): String {
    return "$type/$klassekode ${periode.fraOgMed}–${periode.tilOgMed} $beløp kr ($fagområde)"
}
