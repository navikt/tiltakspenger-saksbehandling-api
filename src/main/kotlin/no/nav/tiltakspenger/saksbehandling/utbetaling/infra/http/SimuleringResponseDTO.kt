package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Kommentar jah: Ser ikke simuleringstypene i kontrakter: https://github.com/navikt/utsjekk-kontrakter/
 * Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L90
 */
private data class SimuleringResponseDTO(
    val oppsummeringer: List<OppsummeringForPeriode>,
    val detaljer: SimuleringDetaljer,
) {

    /**
     * Se også: Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L95
     */
    data class OppsummeringForPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
    )

    data class SimuleringDetaljer(
        val gjelderId: String,
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        val perioder: List<PosteringerForPeriode>,
    ) {
        data class PosteringerForPeriode(
            val fom: LocalDate,
            val tom: LocalDate,
            val posteringer: List<Postering>,
        ) {
            val fagområde: String by lazy { posteringer.map { it.fagområde }.distinct().single() }
            val sakId: String by lazy { posteringer.map { it.sakId }.distinct().single() }
            val fraOgMed: LocalDate by lazy { posteringer.map { it.fom }.distinct().single() }
            val tilOgMed: LocalDate by lazy { posteringer.map { it.tom }.distinct().single() }
            val periode: Periode by lazy { Periode(fraOgMed, tilOgMed) }

            data class Postering(
                val fagområde: String,
                val sakId: String,
                val fom: LocalDate,
                val tom: LocalDate,
                val beløp: Int,
                val type: String,
                val klassekode: String,
            ) {
                val periode: Periode by lazy { Periode(fom, tom) }
                fun typeToDomain(): Posteringstype {
                    return when (type) {
                        "YTELSE" -> Posteringstype.YTELSE
                        "FEILUTBETALING" -> Posteringstype.FEILUTBETALING
                        "FORSKUDSSKATT" -> Posteringstype.FORSKUDSSKATT
                        "JUSTERING" -> Posteringstype.JUSTERING
                        "TREKK" -> Posteringstype.TREKK
                        "MOTPOSTERING" -> Posteringstype.MOTPOSTERING
                        else -> error("Ukjent posteringstype: $type")
                    }
                }
            }
        }
    }
}

fun String.toSimuleringFraHelvedResponse(
    meldeperiodeKjeder: MeldeperiodeKjeder,
): Simulering.Endring {
    return deserialize<SimuleringResponseDTO>(this).let { res ->
        check(Fnr.fromString(res.detaljer.gjelderId) == meldeperiodeKjeder.fnr) {
            "Simulering sin gjelderId: ${res.detaljer.gjelderId} er ulik behandlingens fnr $meldeperiodeKjeder.fnr"
        }
        // TODO jah: Her må vi nok filtrere på vårt fagområde. Antar vi kan få utbetalinger fra flere enn vårt fagområde.
//        res.detaljer.perioder.flatMap { it.posteringer }.map { Saksnummer(it.sakId) }.distinct().let {
//            check(it.size == 1 && it.first() == meldeperiodeKjeder.saksnummer) {
//                "Simulering sin sakId: ${it.joinToString()} er ulik behandlingens saksnummer ${meldeperiodeKjeder.saksnummer}"
//            }
//        }
        OppsummeringGenerator.lagOppsummering(
            res.tilPosteringerPerDag(),
            meldeperiodeKjeder,
            res.detaljer.datoBeregnet,
            res.detaljer.totalBeløp,
        )
    }
}

private fun SimuleringResponseDTO.tilPosteringerPerDag(): Map<LocalDate, PosteringerForDag> {
    return this.detaljer.perioder.flatMap { posteringerForPeriode ->
        val periode = posteringerForPeriode.periode
        val antallDager = periode.antallDager
        periode.tilDager().map { dato ->
            PosteringerForDag(
                dato = dato,
                posteringer = posteringerForPeriode.posteringer.map { postering ->
                    PosteringForDag(
                        dato = dato,
                        fagområde = postering.fagområde,
                        // Vi forventer egentlig et heltall her. Siden vi kun sender heltall per dag og ikke dealer med skatt.
                        beløp = (postering.beløp.toDouble() / antallDager).roundToInt(),
                        type = postering.typeToDomain(),
                        klassekode = postering.klassekode,
                    )
                }.toNonEmptyListOrNull()!!,
            )
        }
    }.sortedBy { it.dato }.also {
        it.zipWithNext { a, b ->
            require(a.dato < b.dato) {
                "Forventer at posteringsdagene er i stigende rekkefølge og ikke har duplikater: ${a.dato} > ${b.dato}"
            }
        }
    }.associateBy { it.dato }
}
