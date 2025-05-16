package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDate

sealed interface SimuleringDTO

data class SimuleringIngenEndringDTO(
    val type: String = "IngenEndring",
) : SimuleringDTO

/**
 * Kun ment brukt i routes mot frontend. Se SimuleringDBJson for hvordan vi lagrer simulering i databasen.
 */
data class SimuleringEndringDTO(
    /** Merk at det kan mangle hele meldeperioder. Og hver enkelt meldeperiode kan ha hull. */
    val totalPeriode: PeriodeDTO,
    val perMeldeperiode: List<SimuleringForMeldeperiode>,
    /** Utregnet */
    val tidligereUtbetalt: Int,
    /** Utregnet */
    val nyUtbetaling: Int,
    /** Utregnet */
    val totalEtterbetaling: Int,
    /** Utregnet */
    val totalFeilutbetaling: Int,
    /** Som det kommer fra OS */
    val totalBeløp: Int,
    /** Som det kommer fra OS */
    val datoBeregnet: LocalDate,
    val type: String = "Endring",
) : SimuleringDTO {
    data class SimuleringForMeldeperiode(
        val meldeperiodeId: String,
        val meldeperiodeKjedeId: String,
        val periode: PeriodeDTO,
        val simuleringsdager: List<Simuleringsdag>,
    )

    data class Simuleringsdag(
        val dato: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
        // Tidligere kalt detaljer. Kan vises i frontend for ekspertbrukere, bør kanskje "skjules" litt mer enn oppsummeringen.
        val posteringsdag: PosteringerForDag,
    )

    data class PosteringerForDag(
        val dato: LocalDate,
        val posteringer: List<PosteringForDag>,
    )

    data class PosteringForDag(
        val dato: LocalDate,
        val fagområde: String,
        val beløp: Int,
        val type: String,
        val klassekode: String,
    )
}

fun Simulering.tilSimuleringDTO(): SimuleringDTO {
    return when (this) {
        is Simulering.IngenEndring -> SimuleringIngenEndringDTO()
        is Simulering.Endring -> SimuleringEndringDTO(
            tidligereUtbetalt = this.tidligereUtbetalt,
            nyUtbetaling = this.nyUtbetaling,
            totalEtterbetaling = this.totalEtterbetaling,
            totalFeilutbetaling = this.totalFeilutbetaling,
            totalBeløp = this.totalBeløp,
            datoBeregnet = this.datoBeregnet,
            totalPeriode = this.totalPeriode.toDTO(),
            perMeldeperiode = this.simuleringPerMeldeperiode.map {
                SimuleringEndringDTO.SimuleringForMeldeperiode(
                    meldeperiodeId = it.meldeperiode.id.toString(),
                    meldeperiodeKjedeId = it.meldeperiode.kjedeId.toString(),
                    periode = it.meldeperiode.periode.toDTO(),
                    simuleringsdager = it.simuleringsdager.map { simuleringsdag ->
                        SimuleringEndringDTO.Simuleringsdag(
                            dato = simuleringsdag.dato,
                            tidligereUtbetalt = simuleringsdag.tidligereUtbetalt,
                            nyUtbetaling = simuleringsdag.nyUtbetaling,
                            totalEtterbetaling = simuleringsdag.totalEtterbetaling,
                            totalFeilutbetaling = simuleringsdag.totalFeilutbetaling,
                            posteringsdag = SimuleringEndringDTO.PosteringerForDag(
                                dato = simuleringsdag.dato,
                                posteringer = simuleringsdag.posteringsdag.posteringer.map { posteringForDag ->
                                    SimuleringEndringDTO.PosteringForDag(
                                        dato = posteringForDag.dato,
                                        fagområde = posteringForDag.fagområde,
                                        beløp = posteringForDag.beløp,
                                        type = posteringForDag.type.toString(),
                                        klassekode = posteringForDag.klassekode,
                                    )
                                },
                            ),
                        )
                    },
                )
            },
        )
    }
}
