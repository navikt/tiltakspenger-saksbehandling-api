package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import java.time.LocalDate

private data class SimuleringMedMetadataDbJson(
    val simulering: SimuleringDbJson?,
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
    val datoBeregnet: LocalDate,
    val totalBeløp: Int,
    val perMeldeperiode: List<SimuleringForMeldeperiode>,
) {
    data class SimuleringForMeldeperiode(
        val meldeperiodeId: String,
        val simuleringsdager: List<Simuleringsdag>,
    )

    data class Simuleringsdag(
        val dato: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
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
        val type: PosteringstypeDbType,
        val klassekode: String,
    )

    enum class PosteringstypeDbType {
        YTELSE,
        FEILUTBETALING,
        FORSKUDSSKATT,
        JUSTERING,
        TREKK,
        MOTPOSTERING,
        ;

        fun toDomain(): Posteringstype {
            return when (this) {
                YTELSE -> Posteringstype.YTELSE
                FEILUTBETALING -> Posteringstype.FEILUTBETALING
                FORSKUDSSKATT -> Posteringstype.FORSKUDSSKATT
                JUSTERING -> Posteringstype.JUSTERING
                TREKK -> Posteringstype.TREKK
                MOTPOSTERING -> Posteringstype.MOTPOSTERING
            }
        }
    }

    fun toEndring(meldeperiodeKjeder: MeldeperiodeKjeder): Simulering.Endring {
        return Simulering.Endring(
            datoBeregnet = this.datoBeregnet,
            totalBeløp = this.totalBeløp,
            simuleringPerMeldeperiode = this.perMeldeperiode.map {
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiodeKjeder.hentForMeldeperiodeId(MeldeperiodeId.fromString(it.meldeperiodeId))!!,
                    simuleringsdager = it.simuleringsdager.map { dag ->
                        Simuleringsdag(
                            dato = dag.dato,
                            tidligereUtbetalt = dag.tidligereUtbetalt,
                            nyUtbetaling = dag.nyUtbetaling,
                            totalEtterbetaling = dag.totalEtterbetaling,
                            totalFeilutbetaling = dag.totalFeilutbetaling,
                            posteringsdag = PosteringerForDag(
                                dato = dag.dato,
                                posteringer = dag.posteringsdag.posteringer.map { posteringForDag ->
                                    PosteringForDag(
                                        dato = posteringForDag.dato,
                                        fagområde = posteringForDag.fagområde,
                                        beløp = posteringForDag.beløp,
                                        type = posteringForDag.type.toDomain(),
                                        klassekode = posteringForDag.klassekode,
                                    )
                                }.toNonEmptyListOrNull()!!,
                            ),
                        )
                    }.toNonEmptyListOrNull()!!,
                )
            }.toNonEmptyListOrNull()!!,
        )
    }
}

internal fun SimuleringMedMetadata.toDbJson(): String {
    return SimuleringMedMetadataDbJson(
        simulering = simulering.toDbJson(),
        type = when (simulering) {
            is Simulering.Endring -> Type.ENDRING
            Simulering.IngenEndring -> Type.INGEN_ENDRING
        },
    ).let { serialize(it) }
}

internal fun String.toSimuleringFraDbJson(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
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
        datoBeregnet = this.datoBeregnet,
        totalBeløp = this.totalBeløp,
        perMeldeperiode = this.simuleringPerMeldeperiode.toList().map {
            SimuleringDbJson.SimuleringForMeldeperiode(
                meldeperiodeId = it.meldeperiode.id.toString(),
                simuleringsdager = it.simuleringsdager.toList().map { dag ->
                    SimuleringDbJson.Simuleringsdag(
                        dato = dag.dato,
                        tidligereUtbetalt = dag.tidligereUtbetalt,
                        nyUtbetaling = dag.nyUtbetaling,
                        totalEtterbetaling = dag.totalEtterbetaling,
                        totalFeilutbetaling = dag.totalFeilutbetaling,
                        posteringsdag = SimuleringDbJson.PosteringerForDag(
                            dato = dag.posteringsdag.dato,
                            posteringer = dag.posteringsdag.posteringer.toList().map { postering ->
                                SimuleringDbJson.PosteringForDag(
                                    dato = postering.dato,
                                    fagområde = postering.fagområde,
                                    beløp = postering.beløp,
                                    type = postering.type.toDbType(),
                                    klassekode = postering.klassekode,
                                )
                            },
                        ),
                    )
                },
            )
        },
    )
}

private fun Posteringstype.toDbType(): SimuleringDbJson.PosteringstypeDbType {
    return when (this) {
        Posteringstype.YTELSE -> SimuleringDbJson.PosteringstypeDbType.YTELSE
        Posteringstype.FEILUTBETALING -> SimuleringDbJson.PosteringstypeDbType.FEILUTBETALING
        Posteringstype.FORSKUDSSKATT -> SimuleringDbJson.PosteringstypeDbType.FORSKUDSSKATT
        Posteringstype.JUSTERING -> SimuleringDbJson.PosteringstypeDbType.JUSTERING
        Posteringstype.TREKK -> SimuleringDbJson.PosteringstypeDbType.TREKK
        Posteringstype.MOTPOSTERING -> SimuleringDbJson.PosteringstypeDbType.MOTPOSTERING
    }
}
