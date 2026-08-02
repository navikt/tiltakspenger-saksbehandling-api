package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Postering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.finnPosteringUtenforMeldeperioden
import java.time.LocalDate
import java.time.LocalDateTime

data class SimuleringDbJson(
    val simulering: SimuleringEndringDbJson?,
    val type: SimuleringTypeDb,
    val simuleringstidspunkt: LocalDateTime,
) {
    init {
        if (type == SimuleringTypeDb.ENDRING) {
            requireNotNull(simulering) { "Simulering må være satt for endring" }
        } else {
            require(simulering == null) { "Simulering må være null for ingen endring" }
        }
    }

    fun toDomain(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
        return when (type) {
            SimuleringTypeDb.ENDRING -> simulering!!.toEndring(hentMeldeperiodekjederForSakId, simuleringstidspunkt)
            SimuleringTypeDb.INGEN_ENDRING -> Simulering.IngenEndring(simuleringstidspunkt)
        }
    }
}

enum class SimuleringTypeDb {
    ENDRING,
    INGEN_ENDRING,
}

/** Kan brukes på tvers av behandlingstyper. */
data class SimuleringEndringDbJson(
    val datoBeregnet: LocalDate,
    val totalBeløp: Int,
    val perMeldeperiode: List<SimuleringForMeldeperiode>,
) {
    data class SimuleringForMeldeperiode(
        val meldeperiodeId: String,
        val simuleringsdager: List<Simuleringsdag>,

        /**
         * Posteringene med perioden oppdragssystemet ga dem.
         * Feltet ble innført da vi sluttet å splitte posteringene opp i dager, se #1734.
         *
         * Rader skrevet før det mangler feltet, og posteringene ligger da under [Simuleringsdag.posteringsdag] i stedet.
         * Tilstedeværelsen av dette feltet er det som skiller de to formene -- vi trenger ikke noe versjonsnummer.
         */
        val posteringer: List<Postering>? = null,
    )

    data class Simuleringsdag(
        val dato: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
        // Denne ble lagt til 30. sept, er antatt å alltid være lik totalFeilutbetaling
        val totalMotpostering: Int = totalFeilutbetaling,
        // Disse feltene ble lagt til 16. september 2025. Får vurdere og migrere de senere eller bare defaulte til 0. Vi har ikke fått noen simuleringer med typene TREKK eller JUSTERING enda.
        val totalTrekk: Int = 0,
        val totalJustering: Int = 0,
        // Denne ble lagt til 30. sept.
        // Defaulten er ikke nødvendigvis korrekt, dersom dagen har både positive og negative justeringer som nuller ut hverandre
        val harJustering: Boolean = totalJustering < 0,

        /**
         * Den gamle formen, der hver postering var splittet opp i én rad per dag.
         * Erstattet av [SimuleringForMeldeperiode.posteringer], og skrives ikke lenger.
         *
         * Feltet leses fortsatt, slik at behandlinger som ble simulert før endringen viser nøyaktig de tallene saksbehandler så den gangen.
         * Vi regner dem bevisst ikke om -- det ville endret tall på lukkede behandlinger i ettertid.
         */
        val posteringsdag: PosteringerForDag? = null,
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

    data class Postering(
        val periode: PeriodeDbJson,
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
}

/**
 * Ligger på toppnivå, ikke inne i [SimuleringEndringDbJson].
 * Grunnen er at de nestede db-typene har samme navn som domenetypene og ville skygget for dem inne i klassekroppen.
 */
private fun SimuleringEndringDbJson.toEndring(
    meldeperiodeKjeder: MeldeperiodeKjeder,
    simuleringstidspunkt: LocalDateTime,
): Simulering.Endring {
    return Simulering.Endring(
        datoBeregnet = this.datoBeregnet,
        totalBeløp = this.totalBeløp,
        simuleringstidspunkt = simuleringstidspunkt,
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
                        totalMotpostering = dag.totalMotpostering,
                        totalTrekk = dag.totalTrekk,
                        totalJustering = dag.totalJustering,
                        harJustering = dag.harJustering,
                    )
                }.toNonEmptyListOrNull()!!,
                posteringer = it.tilPosteringer(),
            ).also { simuleringForMeldeperiode ->
                /*
                  En lagret rad kan være skrevet av en eldre versjon, eller manipulert.
                  Skapeflyten svarer med typet feil på samme invariant; her finnes ingen saksbehandler å svare, så saken skal være utilgjengelig til en utvikler har sett på den.
                 */
                simuleringForMeldeperiode.finnPosteringUtenforMeldeperioden()?.let { feil ->
                    throw IllegalArgumentException(feil.loggkontekst.melding)
                }
            }
        }.toNonEmptyListOrNull()!!,
    )
}

/**
 * Leser posteringene fra begge formene raden kan ha.
 *
 * Er `posteringer` satt, er raden skrevet etter at vi sluttet å splitte posteringene opp i dager, og periodene er de oppdragssystemet ga oss.
 * Mangler feltet, er raden eldre, og hver dagspostering leses som en postering på én dag.
 * Det er ingen rekonstruksjon -- det er nøyaktig det raden inneholder.
 */
private fun SimuleringEndringDbJson.SimuleringForMeldeperiode.tilPosteringer(): NonEmptyList<Postering> {
    posteringer?.let { lagredePosteringer ->
        return lagredePosteringer.map { postering ->
            Postering(
                periode = postering.periode.toDomain(),
                fagområde = postering.fagområde,
                beløp = postering.beløp,
                type = postering.type.toDomain(),
                klassekode = postering.klassekode,
            )
        }.toNonEmptyListOrNull()!!
    }
    return simuleringsdager.flatMap { dag ->
        val posteringsdag = checkNotNull(dag.posteringsdag) {
            "Simuleringsdagen ${dag.dato} har verken posteringer på meldeperioden eller posteringsdag på dagen, og kan ikke tolkes. Meldeperiode: $meldeperiodeId"
        }
        posteringsdag.posteringer.map { posteringForDag ->
            Postering(
                periode = Periode(posteringForDag.dato, posteringForDag.dato),
                fagområde = posteringForDag.fagområde,
                beløp = posteringForDag.beløp,
                type = posteringForDag.type.toDomain(),
                klassekode = posteringForDag.klassekode,
            )
        }
    }.toNonEmptyListOrNull()!!
}

fun SimuleringMedMetadata.toSimuleringDbJson(): SimuleringDbJson = simulering.toSimuleringDbJson()

fun SimuleringMedMetadata.toDbJson(): String {
    return serialize(toSimuleringDbJson())
}

fun Simulering.toSimuleringDbJson(): SimuleringDbJson {
    return SimuleringDbJson(
        simulering = this.toSimuleringEndringDbJson(),
        type = when (this) {
            is Simulering.Endring -> SimuleringTypeDb.ENDRING
            is Simulering.IngenEndring -> SimuleringTypeDb.INGEN_ENDRING
        },
        simuleringstidspunkt = this.simuleringstidspunkt,
    )
}

fun Simulering.toDbJson(): String {
    return serialize(toSimuleringDbJson())
}

fun String.toSimuleringFraDbJson(hentMeldeperiodekjederForSakId: MeldeperiodeKjeder): Simulering {
    return deserialize<SimuleringDbJson>(this).toDomain(hentMeldeperiodekjederForSakId)
}

private fun Simulering.toSimuleringEndringDbJson(): SimuleringEndringDbJson? {
    return when (this) {
        is Simulering.Endring -> toDbJson()
        is Simulering.IngenEndring -> null
    }
}

private fun Simulering.Endring.toDbJson(): SimuleringEndringDbJson {
    return SimuleringEndringDbJson(
        datoBeregnet = this.datoBeregnet,
        totalBeløp = this.totalBeløp,
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
                        totalMotpostering = dag.totalMotpostering,
                        totalTrekk = dag.totalTrekk,
                        totalJustering = dag.totalJustering,
                        harJustering = dag.harJustering,
                    )
                },
                posteringer = it.posteringer.toList().map { postering ->
                    SimuleringEndringDbJson.Postering(
                        periode = postering.periode.toDbJson(),
                        fagområde = postering.fagområde,
                        beløp = postering.beløp,
                        type = postering.type.toDbType(),
                        klassekode = postering.klassekode,
                    )
                },
            )
        },
    )
}

fun Posteringstype.toDbType(): SimuleringEndringDbJson.PosteringstypeDbType {
    return when (this) {
        Posteringstype.YTELSE -> SimuleringEndringDbJson.PosteringstypeDbType.YTELSE
        Posteringstype.FEILUTBETALING -> SimuleringEndringDbJson.PosteringstypeDbType.FEILUTBETALING
        Posteringstype.FORSKUDSSKATT -> SimuleringEndringDbJson.PosteringstypeDbType.FORSKUDSSKATT
        Posteringstype.JUSTERING -> SimuleringEndringDbJson.PosteringstypeDbType.JUSTERING
        Posteringstype.TREKK -> SimuleringEndringDbJson.PosteringstypeDbType.TREKK
        Posteringstype.MOTPOSTERING -> SimuleringEndringDbJson.PosteringstypeDbType.MOTPOSTERING
    }
}
