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
    val oppsummering: Oppsummering,
    val detaljer: Detaljer,
    val type: String = "Endring",
) : SimuleringDTO {
    data class Oppsummering(
        val periode: PeriodeDTO,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
        val perMeldeperiode: List<OppsummeringForMeldeperiode>,
    )

    data class OppsummeringForMeldeperiode(
        val periode: PeriodeDTO,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
    )

    data class Detaljer(
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        val perioder: List<Simuleringsperiode>,
    ) {
        data class Simuleringsperiode(
            val periode: PeriodeDTO,
            val delperiode: List<Delperiode>,
        ) {
            data class Delperiode(
                val fagområde: String,
                val periode: PeriodeDTO,
                val beløp: Int,
                val type: String,
                val klassekode: String,
            )
        }
    }
}

fun Simulering.tilSimuleringDTO(): SimuleringDTO {
    return when (this) {
        is Simulering.IngenEndring -> SimuleringIngenEndringDTO()
        is Simulering.Endring -> SimuleringEndringDTO(
            oppsummering = SimuleringEndringDTO.Oppsummering(
                periode = this.oppsummering.periode.toDTO(),
                tidligereUtbetalt = this.oppsummering.tidligereUtbetalt,
                nyUtbetaling = this.oppsummering.nyUtbetaling,
                totalEtterbetaling = this.oppsummering.totalEtterbetaling,
                totalFeilutbetaling = this.oppsummering.totalFeilutbetaling,
                perMeldeperiode = this.oppsummering.perMeldeperiode.map {
                    SimuleringEndringDTO.OppsummeringForMeldeperiode(
                        periode = it.meldeperiode.toDTO(),
                        tidligereUtbetalt = it.tidligereUtbetalt,
                        nyUtbetaling = it.nyUtbetaling,
                        totalEtterbetaling = it.totalEtterbetaling,
                        totalFeilutbetaling = it.totalFeilutbetaling,
                    )
                },
            ),
            detaljer = SimuleringEndringDTO.Detaljer(
                datoBeregnet = this.detaljer.datoBeregnet,
                totalBeløp = this.detaljer.totalBeløp,
                perioder = this.detaljer.perioder.map {
                    SimuleringEndringDTO.Detaljer.Simuleringsperiode(
                        periode = it.periode.toDTO(),
                        delperiode = it.delperioder.map { delperiode ->
                            SimuleringEndringDTO.Detaljer.Simuleringsperiode.Delperiode(
                                fagområde = delperiode.fagområde,
                                periode = delperiode.periode.toDTO(),
                                beløp = delperiode.beløp,
                                // TODO jah: Lag egen type for dette
                                type = delperiode.type.toString(),
                                klassekode = delperiode.klassekode,
                            )
                        },
                    )
                },
            ),
        )
    }
}
