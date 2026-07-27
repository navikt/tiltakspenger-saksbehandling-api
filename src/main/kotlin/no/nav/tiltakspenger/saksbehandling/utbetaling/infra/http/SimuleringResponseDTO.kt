package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Kommentar jah: Ser ikke simuleringstypene i kontrakter: https://github.com/navikt/utsjekk-kontrakter/
 * Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L90
 */
internal data class SimuleringResponseDTO(
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

            val periode: Periode = Periode(fom, tom)

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

            init {
                if (posteringer.isNotEmpty()) {
                    require(fom == fraOgMed && tom == tilOgMed) {
                        "Periodene for posteringer må være like perioden de tilhører. Forventet $fom - $tom, fikk $fraOgMed - $tilOgMed"
                    }
                }
            }
        }
    }
}

internal fun SimuleringResponseDTO.toSimuleringFraHelvedResponse(
    meldeperiodeKjeder: MeldeperiodeKjeder,
    clock: Clock,
): Simulering {
    return this.let { res ->
        if (res.detaljer.perioder.isEmpty()) {
            return Simulering.IngenEndring(nå(clock))
        }
        check(Fnr.fromString(res.detaljer.gjelderId) == meldeperiodeKjeder.fnr) {
            "Simulering sin gjelderId er ulik behandlingens fnr. sakId: ${meldeperiodeKjeder.sakId}, saksnummer: ${meldeperiodeKjeder.saksnummer}"
        }
        res.detaljer.perioder.flatMap { it.posteringer }.filter { it.fagområde == "TILTAKSPENGER" }
            .map { Saksnummer(it.sakId) }.distinct().let {
                check(it.size == 1 && it.first() == meldeperiodeKjeder.saksnummer) {
                    "Simulering sin sakId: ${it.joinToString()} er ulik behandlingens saksnummer ${meldeperiodeKjeder.saksnummer}. sakId: ${meldeperiodeKjeder.sakId}, saksnummer: ${meldeperiodeKjeder.saksnummer}"
                }
            }
        OppsummeringGenerator.lagOppsummering(
            posteringerPerDag = res.tilPosteringerPerDag(),
            meldeperiodeKjeder = meldeperiodeKjeder,
            datoBeregnet = res.detaljer.datoBeregnet,
            totalBeløp = res.detaljer.totalBeløp,
            clock = clock,
        )
    }
}

/**
 * Splitter posteringene opp i dager, ved å fordele beløpet jevnt over dagene i posteringens periode.
 *
 * Fordelingen er eksakt for ytelsesposteringer.
 * Beregningen vår opererer i hele kroner per dag, og OS slår sammen sammenhengende dager med samme dagsbeløp.
 * Beløpet går derfor opp i antall dager, og divisjonen gir dagsatsen tilbake.
 *
 * For FEILUTBETALING, MOTPOSTERING og JUSTERING er den derimot en **tilnærming**.
 * Disse er resultatet av at OS motregner på tvers av dager, og beløpet svarer ofte til bare en del av perioden det er stemplet med.
 * Et uttrekk fra dev viste for eksempel en reduksjon på fire like dager splittet i én justering på tre dagers verdi og én feilutbetaling på én dags verdi -- begge stemplet med hele firedagersperioden.
 * Da finnes det ingen dagsfordeling å gjenskape, og avrundingen per dag kan gjøre at summen av dagene avviker med noen kroner fra beløpet OS sendte.
 *
 * Avviket var lite i uttrekket (14 av 5182 posteringer, maks to kroner), men det slår inn på sammenligningen mot kontrollsimuleringen og på summene per meldeperiode.
 * Se issue om å modellere posteringene nærmere kilden.
 */
private fun SimuleringResponseDTO.tilPosteringerPerDag(): Map<LocalDate, PosteringerForDag> {
    return this.detaljer.perioder.flatMap { posteringerForPeriode ->
        val periode = posteringerForPeriode.periode
        val antallDager = periode.antallDager
        periode.tilDager().map { dato ->
            PosteringerForDag(
                dato = dato,
                posteringer = posteringerForPeriode.posteringer.mapNotNull { postering ->
                    if (postering.fagområde != "TILTAKSPENGER") {
                        // Fjerner alle posteringer som ikke er tiltakspenger.
                        return@mapNotNull null
                    }
                    PosteringForDag(
                        dato = dato,
                        fagområde = postering.fagområde,
                        // Vi forventer egentlig et heltall her.
                        // Siden vi kun sender heltall per dag og ikke dealer med skatt.
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
