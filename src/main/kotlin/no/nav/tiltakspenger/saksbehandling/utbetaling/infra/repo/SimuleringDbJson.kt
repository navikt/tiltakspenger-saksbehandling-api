package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.FEILUTBETALING
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.FORSKUDSSKATT
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.JUSTERING
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.MOTPOSTERING
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.TREKK
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson.PosteringType.YTELSE
import java.time.LocalDate

private data class SimuleringMedMetadataDbJson(
    val simulering: SimuleringDbJson?,
    val originalJson: String,
    val type: Type,
) {
    init {
        if (type == Type.ENDRING) {
            requireNotNull(simulering) { "Simulering må være satt for endring" }
        } else {
            require(simulering == null) { "Simulering må være null for ingen endring" }
        }
    }

    fun toDomain(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
        return when (type) {
            Type.ENDRING -> simulering!!.toEndring(hentMeldeperiodekjederForSakId)
            Type.INGEN_ENDRING -> Simulering.IngenEndring
        }
    }
}

private enum class Type {
    ENDRING,
    INGEN_ENDRING,
}

/** Kan brukes på tvers av behandlingstyper. */
private data class SimuleringDbJson(
    val oppsummering: Oppsummering,
    val detaljer: Detaljer,
) {
    data class Oppsummering(
        val periode: PeriodeDbJson,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
        val perMeldeperiode: List<OppsummeringForMeldeperiode>,
    )

    data class OppsummeringForMeldeperiode(
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
                val type: PosteringType,
                val klassekode: String,
            )
        }
    }

    enum class PosteringType {
        YTELSE,
        FEILUTBETALING,
        FORSKUDSSKATT,
        JUSTERING,
        TREKK,
        MOTPOSTERING,
        ;

        fun toDomain(): Simulering.Endring.PosteringType {
            return when (this) {
                YTELSE -> Simulering.Endring.PosteringType.YTELSE
                FEILUTBETALING -> Simulering.Endring.PosteringType.FEILUTBETALING
                FORSKUDSSKATT -> Simulering.Endring.PosteringType.FORSKUDSSKATT
                JUSTERING -> Simulering.Endring.PosteringType.JUSTERING
                TREKK -> Simulering.Endring.PosteringType.TREKK
                MOTPOSTERING -> Simulering.Endring.PosteringType.MOTPOSTERING
            }
        }
    }

    fun toEndring(meldeperiodeKjeder: MeldeperiodeKjeder): Simulering.Endring {
        val domeneDetaljer = Simulering.Endring.Detaljer(
            datoBeregnet = detaljer.datoBeregnet,
            totalBeløp = detaljer.totalBeløp,
            perioder = detaljer.perioder.map {
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = it.periode.toDomain(),
                    delperioder = it.posteringer.map { postering ->
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = postering.fagområde,
                            periode = postering.periode.toDomain(),
                            beløp = postering.beløp,
                            type = postering.type.toDomain(),
                            klassekode = postering.klassekode,
                        )
                    },
                )
            }.toNonEmptyListOrNull()!!,
        )
        return Simulering.Endring(
            // TODO jah: Inntil oppsummering har modnet, så persisterer vi den, men genererer den på nytt.
            oppsummering = OppsummeringGenerator.lagOppsummering(domeneDetaljer, meldeperiodeKjeder),
            detaljer = domeneDetaljer,
        )
    }
}

internal fun SimuleringMedMetadata.toDbJson(): String {
    return SimuleringMedMetadataDbJson(
        simulering = simulering.toDbJson(),
        originalJson = originalJson,
        type = when (simulering) {
            is Simulering.Endring -> Type.ENDRING
            Simulering.IngenEndring -> Type.INGEN_ENDRING
        },
    ).let { serialize(it) }
}

internal fun String.toSimulering(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
    return deserialize<SimuleringMedMetadataDbJson>(this).toDomain(hentMeldeperiodekjederForSakId)
}

private fun Simulering.toDbJson(): SimuleringDbJson? {
    return when (this) {
        is Simulering.Endring -> toDbJson()
        Simulering.IngenEndring -> null
    }
}

private fun Simulering.Endring.toDbJson(): SimuleringDbJson {
    return SimuleringDbJson(
        oppsummering = SimuleringDbJson.Oppsummering(
            periode = this.oppsummering.periode.toDbJson(),
            tidligereUtbetalt = this.oppsummering.tidligereUtbetalt,
            nyUtbetaling = this.oppsummering.nyUtbetaling,
            totalEtterbetaling = this.oppsummering.totalEtterbetaling,
            totalFeilutbetaling = this.oppsummering.totalFeilutbetaling,
            perMeldeperiode = oppsummering.perMeldeperiode.map {
                SimuleringDbJson.OppsummeringForMeldeperiode(
                    periode = it.meldeperiode.toDbJson(),
                    tidligereUtbetalt = it.tidligereUtbetalt,
                    nyUtbetaling = it.nyUtbetaling,
                    totalEtterbetaling = it.totalEtterbetaling,
                    totalFeilutbetaling = it.totalFeilutbetaling,
                )
            },
        ),
        detaljer = SimuleringDbJson.Detaljer(
            datoBeregnet = detaljer.datoBeregnet,
            totalBeløp = detaljer.totalBeløp,
            perioder = detaljer.perioder.map {
                SimuleringDbJson.Detaljer.Simuleringsperiode(
                    periode = it.periode.toDbJson(),
                    posteringer = it.delperioder.map { postering ->
                        SimuleringDbJson.Detaljer.Simuleringsperiode.Postering(
                            fagområde = postering.fagområde,
                            periode = postering.periode.toDbJson(),
                            beløp = postering.beløp,
                            type = postering.type.toDbType(),
                            klassekode = postering.klassekode,
                        )
                    },
                )
            },
        ),
    )
}

private fun Simulering.Endring.PosteringType.toDbType(): PosteringType {
    return when (this) {
        Simulering.Endring.PosteringType.YTELSE -> YTELSE
        Simulering.Endring.PosteringType.FEILUTBETALING -> FEILUTBETALING
        Simulering.Endring.PosteringType.FORSKUDSSKATT -> FORSKUDSSKATT
        Simulering.Endring.PosteringType.JUSTERING -> JUSTERING
        Simulering.Endring.PosteringType.TREKK -> TREKK
        Simulering.Endring.PosteringType.MOTPOSTERING -> MOTPOSTERING
    }
}
