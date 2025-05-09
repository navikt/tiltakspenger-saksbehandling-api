package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDate

/**
 * Kun ment brukt i routes mot frontend. Se SimuleringDBJson for hvordan vi lagrer simulering i databasen.
 */
data class SimuleringDTO(
    val oppsummeringForPerioder: List<OppsummeringForPeriode>,
    val detaljer: Detaljer,
) {
    data class OppsummeringForPeriode(
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
    return SimuleringDTO(
        oppsummeringForPerioder = this.oppsummeringForPerioder.map {
            SimuleringDTO.OppsummeringForPeriode(
                periode = it.periode.toDTO(),
                tidligereUtbetalt = it.tidligereUtbetalt,
                nyUtbetaling = it.nyUtbetaling,
                totalEtterbetaling = it.totalEtterbetaling,
                totalFeilutbetaling = it.totalFeilutbetaling,
            )
        },
        detaljer = SimuleringDTO.Detaljer(
            datoBeregnet = this.detaljer.datoBeregnet,
            totalBeløp = this.detaljer.totalBeløp,
            perioder = this.detaljer.perioder.map {
                SimuleringDTO.Detaljer.Simuleringsperiode(
                    periode = it.periode.toDTO(),
                    delperiode = it.posteringer.map { delperiode ->
                        SimuleringDTO.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = delperiode.fagområde,
                            periode = delperiode.periode.toDTO(),
                            beløp = delperiode.beløp,
                            type = delperiode.type,
                            klassekode = delperiode.klassekode,
                        )
                    },
                )
            },
        ),
    )
}
