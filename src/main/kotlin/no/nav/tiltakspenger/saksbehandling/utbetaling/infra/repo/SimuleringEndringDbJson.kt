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
import java.time.LocalDate

private data class SimuleringDbJson(
    val simulering: SimuleringEndringDbJson?,
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
private data class SimuleringEndringDbJson(
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
        // Disse feltene ble lagt til 16. september 2025. Får vurdere og migrere de senere eller bare defaulte til 0. Vi har ikke fått noen simuleringer med typene TREKK eller JUSTERING enda.
        val totalTrekk: Int = 0,
        val totalJustering: Int = 0,
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
            eksternDatoBeregnet = this.datoBeregnet,
            eksterntTotalbeløp = this.totalBeløp,
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
                            totalTrekk = dag.totalTrekk,
                            totalJustering = dag.totalJustering,
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
    return SimuleringDbJson(
        simulering = simulering.toSimuleringEndringDbJson(),
        type = when (simulering) {
            is Simulering.Endring -> Type.ENDRING
            Simulering.IngenEndring -> Type.INGEN_ENDRING
        },
    ).let { serialize(it) }
}

internal fun Simulering.toDbJson(): String {
    return SimuleringDbJson(
        simulering = this.toSimuleringEndringDbJson(),
        type = when (this) {
            is Simulering.Endring -> Type.ENDRING
            Simulering.IngenEndring -> Type.INGEN_ENDRING
        },
    ).let { serialize(it) }
}

internal fun String.toSimuleringFraDbJson(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
    return deserialize<SimuleringDbJson>(this).toDomain(hentMeldeperiodekjederForSakId)
}

private fun Simulering.toSimuleringEndringDbJson(): SimuleringEndringDbJson? {
    return when (this) {
        is Simulering.Endring -> toDbJson()
        Simulering.IngenEndring -> null
    }
}

private fun Simulering.Endring.toDbJson(): SimuleringEndringDbJson {
    return SimuleringEndringDbJson(
        datoBeregnet = this.eksternDatoBeregnet,
        totalBeløp = this.eksterntTotalbeløp,
        perMeldeperiode = this.simuleringPerMeldeperiode.toList().map {
            SimuleringEndringDbJson.SimuleringForMeldeperiode(
                meldeperiodeId = it.meldeperiode.id.toString(),
                simuleringsdager = it.simuleringsdager.toList().map { dag ->
                    SimuleringEndringDbJson.Simuleringsdag(
                        dato = dag.dato,
                        tidligereUtbetalt = dag.tidligereUtbetalt,
                        nyUtbetaling = dag.nyUtbetaling,
                        totalEtterbetaling = dag.totalEtterbetaling,
                        totalFeilutbetaling = dag.totalFeilutbetaling,
                        totalTrekk = dag.totalTrekk,
                        totalJustering = dag.totalJustering,
                        posteringsdag = SimuleringEndringDbJson.PosteringerForDag(
                            dato = dag.posteringsdag.dato,
                            posteringer = dag.posteringsdag.posteringer.toList().map { postering ->
                                SimuleringEndringDbJson.PosteringForDag(
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

private fun Posteringstype.toDbType(): SimuleringEndringDbJson.PosteringstypeDbType {
    return when (this) {
        Posteringstype.YTELSE -> SimuleringEndringDbJson.PosteringstypeDbType.YTELSE
        Posteringstype.FEILUTBETALING -> SimuleringEndringDbJson.PosteringstypeDbType.FEILUTBETALING
        Posteringstype.FORSKUDSSKATT -> SimuleringEndringDbJson.PosteringstypeDbType.FORSKUDSSKATT
        Posteringstype.JUSTERING -> SimuleringEndringDbJson.PosteringstypeDbType.JUSTERING
        Posteringstype.TREKK -> SimuleringEndringDbJson.PosteringstypeDbType.TREKK
        Posteringstype.MOTPOSTERING -> SimuleringEndringDbJson.PosteringstypeDbType.MOTPOSTERING
    }
}
