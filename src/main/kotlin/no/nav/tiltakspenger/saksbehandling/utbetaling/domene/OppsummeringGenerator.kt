package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import java.time.Clock
import java.time.LocalDate
import kotlin.math.abs

/**
 * https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md
 */
object OppsummeringGenerator {

    /**
     * Beløpet en enkelt postering bidrar med på én bestemt dag.
     * Dette er en utledet visningsverdi, ikke noe oppdragssystemet har sagt.
     */
    private data class Posteringsbeløp(
        val type: Posteringstype,
        val klassekode: String,
        val beløp: Int,
    )

    fun lagOppsummering(
        posteringer: List<Postering>,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        datoBeregnet: LocalDate,
        totalBeløp: Int,
        clock: Clock,
    ): Simulering.Endring {
        val simuleringsperiode = Periode(
            fraOgMed = posteringer.minOf { it.periode.fraOgMed },
            tilOgMed = posteringer.maxOf { it.periode.tilOgMed },
        )
        // Merk at simuleringsperioden og meldeperiodene sin totale periode ikke trenger å være like.
        val aktuelleMeldeperioder = meldeperiodeKjeder.hentMeldeperioderForPeriode(simuleringsperiode)
        return Simulering.Endring(
            datoBeregnet = datoBeregnet,
            totalBeløp = totalBeløp,
            simuleringstidspunkt = nå(clock),
            simuleringPerMeldeperiode = aktuelleMeldeperioder.mapNotNull { meldeperiode ->
                val posteringerForMeldeperiode = posteringer
                    .filter { it.periode.overlapperMed(meldeperiode.periode) }
                    .onEach { it.validerLiggerInnenfor(meldeperiode) }
                    .toNonEmptyListOrNull() ?: return@mapNotNull null

                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    simuleringsdager = posteringerForMeldeperiode.tilSimuleringsdager(),
                    posteringer = posteringerForMeldeperiode,
                )
            }.toNonEmptyListOrNull()!!,
        )
    }

    /**
     * Utleder dagsverdiene som vises til saksbehandler.
     * Hver postering fordeles eksakt over dagene sine, slik at summen av dagene er lik posteringsbeløpet.
     */
    private fun NonEmptyList<Postering>.tilSimuleringsdager(): NonEmptyList<Simuleringsdag> {
        val posteringerPerDag: Map<LocalDate, List<Posteringsbeløp>> = this.toList()
            .flatMap { postering ->
                postering.beløpPerDag().map { (dato, beløp) ->
                    dato to Posteringsbeløp(postering.type, postering.klassekode, beløp)
                }
            }
            .groupBy({ it.first }, { it.second })

        return posteringerPerDag.entries.sortedBy { it.key }.map { (dato, posteringsbeløp) ->
            Simuleringsdag(
                dato = dato,
                tidligereUtbetalt = beregnTidligereUtbetalt(posteringsbeløp),
                nyUtbetaling = beregnNyttBeløp(posteringsbeløp),
                totalEtterbetaling = beregnEtterbetaling(posteringsbeløp),
                totalFeilutbetaling = beregnFeilutbetaling(posteringsbeløp),
                totalMotpostering = beregnMotposteringer(posteringsbeløp),
                totalTrekk = beregnTrekk(posteringsbeløp),
                totalJustering = beregnJustering(posteringsbeløp),
                harJustering = harJustering(posteringsbeløp),
            )
        }.toNonEmptyListOrNull()!!
    }

    /**
     * Vi sender utbetalingslinjene per meldeperiodekjede, og oppdragssystemet arver de grensene.
     * En postering skal derfor alltid ligge innenfor én meldeperiode.
     * Justeringer skal i tillegg ligge innenfor én kalendermåned, siden oppdrag kun justerer innenfor måneden og hjemmelsvernet grupperer på måned.
     *
     * Holder ikke dette, er dagsverdiene vi utleder oppdiktet, og summene per meldeperiode og måned blir feil uten at noe synes.
     * Da vil vi heller feile høylytt.
     * Oppdragssystemet vurderer å gå fra kalendermåned til 14 dagers frekvens, og denne sjekken er det som gjør det skiftet synlig for oss.
     */
    private fun Postering.validerLiggerInnenfor(meldeperiode: Meldeperiode) {
        require(meldeperiode.periode.inneholderHele(this.periode)) {
            "Posteringen ${this.periode} går utover meldeperioden ${meldeperiode.periode}. Da kan vi ikke fordele beløpet på dager uten å gjette. Meldeperiode: ${meldeperiode.id}, klassekode: ${this.klassekode}, type: ${this.type}"
        }
        require(!this.erJustering || this.periode.fraOgMed.month == this.periode.tilOgMed.month) {
            "Justeringen ${this.periode} går over et månedsskifte. Hjemmelsvernet grupperer justeringer per måned, og da kan vi ikke avgjøre hvilken måned beløpet hører til. Meldeperiode: ${meldeperiode.id}, klassekode: ${this.klassekode}"
        }
    }

    private fun beregnTidligereUtbetalt(posteringer: List<Posteringsbeløp>): Int =
        abs(posteringer.summerBareNegativePosteringer(Posteringstype.YTELSE))

    private fun beregnNyttBeløp(posteringer: List<Posteringsbeløp>): Int =
        posteringer.summerBarePositivePosteringer(Posteringstype.YTELSE) - posteringer.summerBarePositivePosteringer(
            Posteringstype.FEILUTBETALING,
            KLASSEKODE_FEILUTBETALING,
        )

    private fun beregnEtterbetaling(posteringer: List<Posteringsbeløp>): Int {
        val justeringer: Int = beregnJustering(posteringer)
        val resultat = beregnNyttBeløp(posteringer) - beregnTidligereUtbetalt(posteringer)
        return if (justeringer < 0) {
            maxOf(resultat - abs(justeringer), 0)
        } else {
            maxOf(resultat, 0)
        }
    }

    private fun beregnFeilutbetaling(posteringer: List<Posteringsbeløp>): Int =
        maxOf(0, posteringer.summerBarePositivePosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_FEILUTBETALING))

    private fun beregnMotposteringer(posteringer: List<Posteringsbeløp>): Int =
        posteringer.summerPosteringer(Posteringstype.MOTPOSTERING)

    /**
     * TREK i OS/UR, for eksempel trekk fra namsmannen, kreditorer eller forskuddsskatt.
     *
     * Trekk kommer med begge fortegn, og de aller fleste er negative.
     * I et prod-uttrekk var 3 214 av 3 482 trekkposteringer negative, og de positive ser ut til å være reverseringer av tidligere trekk.
     * Vi summerer derfor uansett fortegn, slik at feltet viser det beløpet som faktisk trekkes fra utbetalingen.
     */
    private fun beregnTrekk(posteringer: List<Posteringsbeløp>): Int =
        posteringer.summerPosteringer(Posteringstype.TREKK)

    /**
     * Dersom den er negativ for denne dagen, vil den redusere etterbetalingen.
     * Vi får et innslag per positive justering på andre dager.
     * Dersom den er positiv for denne dagen, vil dagen være justert istedenfor at den fører til feilubetaling.
     * Utregning: tidligere utbetalt = ny utbetaling + justering.
     * Dette er "motregninger" som må sees på tvers av dager.
     * Disse vil komme uten en MOTP.
     *
     * Vi kjenner igjen justeringer på **klassekoden alene**, ikke på posteringstypen.
     * Grunnen er at OS har sendt samme begrep under to ulike typer: som `FEILUTBETALING` med denne klassekoden, og som `JUSTERING` med denne klassekoden.
     * Klassekoden har ligget fast i alt materiale vi har sett, posteringstypen har ikke det.
     * Filtrerer vi på typen, mister vi justeringene stilltiende neste gang OS veksler -- og med dem `harJusteringPåTversAvMeldeperioderEllerMåneder`, som er vernet mot å justere uten hjemmel.
     */
    private fun beregnJustering(posteringer: List<Posteringsbeløp>): Int =
        posteringer.summerPosteringer(KLASSEKODE_JUSTERING)

    private fun harJustering(posteringer: List<Posteringsbeløp>): Boolean =
        posteringer.any { it.klassekode == KLASSEKODE_JUSTERING }

    private fun List<Posteringsbeløp>.summerBarePositivePosteringer(type: Posteringstype): Int =
        this.filter { it.beløp > 0 && it.type == type }.sumOf { it.beløp }

    private fun List<Posteringsbeløp>.summerBareNegativePosteringer(type: Posteringstype): Int =
        this.filter { it.beløp < 0 && it.type == type }.sumOf { it.beløp }

    private fun List<Posteringsbeløp>.summerBarePositivePosteringer(type: Posteringstype, klassekode: String): Int =
        this.filter { it.beløp > 0 && it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    private fun List<Posteringsbeløp>.summerPosteringer(klassekode: String): Int =
        this.filter { it.klassekode == klassekode }.sumOf { it.beløp }

    private fun List<Posteringsbeløp>.summerPosteringer(type: Posteringstype): Int =
        this.filter { it.type == type }.sumOf { it.beløp }

    const val KLASSEKODE_JUSTERING = "KL_KODE_JUST_ARBYT"
    const val KLASSEKODE_FEILUTBETALING = "KL_KODE_FEIL_ARBYT"
}
