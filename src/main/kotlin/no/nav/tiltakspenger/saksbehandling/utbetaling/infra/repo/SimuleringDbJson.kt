package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.LocalDate

private data class SimuleringMedMetadataDbJson(
    val simulering: SimuleringDbJson,
    val originalJson: String,
)

/** Kan brukes på tvers av behandlingstyper. */
private data class SimuleringDbJson(
    val oppsummeringForPerioder: List<OppsummeringForPeriode>,
    val detaljer: Detaljer,
) {
    data class OppsummeringForPeriode(
        val periode: PeriodeDbJson,
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
            val periode: PeriodeDbJson,
            val posteringer: List<Postering>,
        ) {
            data class Postering(
                val fagområde: String,
                val periode: PeriodeDbJson,
                val beløp: Int,
                val type: String,
                val klassekode: String,
            )
        }
    }

    fun toDomain(): Simulering {
        return Simulering(
            oppsummeringForPerioder = oppsummeringForPerioder.map {
                Simulering.OppsummeringForPeriode(
                    periode = it.periode.toDomain(),
                    tidligereUtbetalt = it.tidligereUtbetalt,
                    nyUtbetaling = it.nyUtbetaling,
                    totalEtterbetaling = it.totalEtterbetaling,
                    totalFeilutbetaling = it.totalFeilutbetaling,
                )
            },
            detaljer = Simulering.Detaljer(
                datoBeregnet = detaljer.datoBeregnet,
                totalBeløp = detaljer.totalBeløp,
                perioder = detaljer.perioder.map {
                    Simulering.Detaljer.Simuleringsperiode(
                        periode = it.periode.toDomain(),
                        posteringer = it.posteringer.map { postering ->
                            Simulering.Detaljer.Simuleringsperiode.Postering(
                                fagområde = postering.fagområde,
                                periode = postering.periode.toDomain(),
                                beløp = postering.beløp,
                                type = postering.type,
                                klassekode = postering.klassekode,
                            )
                        },
                    )
                },
            ),
        )
    }
}

internal fun SimuleringMedMetadata.toDbJson(): String {
    return SimuleringMedMetadataDbJson(
        simulering = simulering.toDbJson(),
        originalJson = originalJson,
    ).let { serialize(it) }
}

internal fun String.toSimuleringMedMetadata(): SimuleringMedMetadata {
    return deserialize<SimuleringMedMetadataDbJson>(this).let { dbJson ->
        SimuleringMedMetadata(
            simulering = dbJson.simulering.toDomain(),
            originalJson = dbJson.originalJson,
        )
    }
}

private fun Simulering.toDbJson(): SimuleringDbJson {
    return SimuleringDbJson(
        oppsummeringForPerioder = oppsummeringForPerioder.map {
            SimuleringDbJson.OppsummeringForPeriode(
                periode = it.periode.toDbJson(),
                tidligereUtbetalt = it.tidligereUtbetalt,
                nyUtbetaling = it.nyUtbetaling,
                totalEtterbetaling = it.totalEtterbetaling,
                totalFeilutbetaling = it.totalFeilutbetaling,
            )
        },
        detaljer = SimuleringDbJson.Detaljer(
            datoBeregnet = detaljer.datoBeregnet,
            totalBeløp = detaljer.totalBeløp,
            perioder = detaljer.perioder.map {
                SimuleringDbJson.Detaljer.Simuleringsperiode(
                    periode = it.periode.toDbJson(),
                    posteringer = it.posteringer.map { postering ->
                        SimuleringDbJson.Detaljer.Simuleringsperiode.Postering(
                            fagområde = postering.fagområde,
                            periode = postering.periode.toDbJson(),
                            beløp = postering.beløp,
                            type = postering.type,
                            klassekode = postering.klassekode,
                        )
                    },
                )
            },
        ),
    )
}
