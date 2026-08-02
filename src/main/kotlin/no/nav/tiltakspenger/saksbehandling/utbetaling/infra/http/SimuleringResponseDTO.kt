package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Postering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsfeil
import java.time.Clock
import java.time.LocalDate

/**
 * Speiler `api.SimuleringRespons`, som er typen helved serialiserer og sender oss.
 * Alle typene under ligger i [SimuleringModels.kt](https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringModels.kt).
 *
 * Den fila har tre objekter, og bare to av dem er på tråden mot oss.
 * `api` er konvolutten helved eksponerer, og `domain` er den interne modellen -- men den er `@Serializable` og ligger inne i `detaljer`, så den er like mye en del av kontrakten vår.
 * `client` er helveds egne DTO-er mot oppdragssystemet og treffer oss aldri.
 * Derfor peker konvolutten og oppsummeringene på `api`, mens detaljene peker på `domain`.
 */
data class SimuleringResponseDTO(
    val oppsummeringer: List<OppsummeringForPeriode>,
    val detaljer: SimuleringDetaljer,
) {

    /** Speiler `api.OppsummeringForPeriode`. */
    data class OppsummeringForPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
    )

    /** Speiler `domain.SimuleringDetaljer`, som er typen `api.SimuleringRespons.detaljer` har. */
    data class SimuleringDetaljer(
        val gjelderId: String,
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        val perioder: List<PosteringerForPeriode>,
    ) {
        /** Speiler `domain.Periode`. */
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

            /**
             * Speiler `domain.Postering`.
             *
             * Merk at `fagområde` og `type` er enumer hos helved -- `domain.Fagområde` og `domain.PosteringType` -- mens vi tar imot dem som `String`.
             * Det er med vilje: en ny verdi fra oppdragssystemet skal ikke få deserialiseringen til å ryke.
             * `type` oversettes til [Posteringstype] i [typeToDomain], som feiler eksplisitt på ukjente verdier.
             */
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

fun SimuleringResponseDTO.toSimuleringFraHelvedResponse(
    meldeperiodeKjeder: MeldeperiodeKjeder,
    clock: Clock,
): Either<Simuleringsfeil, Simulering> {
    if (detaljer.perioder.isEmpty()) {
        return Simulering.IngenEndring(nå(clock)).right()
    }
    if (Fnr.fromString(detaljer.gjelderId) != meldeperiodeKjeder.fnr) {
        return Simuleringsfeil.GjelderAnnenPerson(
            sakId = meldeperiodeKjeder.sakId,
            saksnummer = meldeperiodeKjeder.saksnummer,
        ).left()
    }
    val saksnummerISimulering = detaljer.perioder.flatMap { it.posteringer }
        .filter { it.fagområde == "TILTAKSPENGER" }
        .map { Saksnummer(it.sakId) }
        .distinct()
    if (saksnummerISimulering != listOf(meldeperiodeKjeder.saksnummer)) {
        return Simuleringsfeil.GjelderAnnenSak(
            sakId = meldeperiodeKjeder.sakId,
            saksnummer = meldeperiodeKjeder.saksnummer,
            saksnummerISimulering = saksnummerISimulering,
        ).left()
    }
    return OppsummeringGenerator.lagOppsummering(
        posteringer = tilPosteringer(),
        meldeperiodeKjeder = meldeperiodeKjeder,
        datoBeregnet = detaljer.datoBeregnet,
        totalBeløp = detaljer.totalBeløp,
        clock = clock,
    )
}

/**
 * Plukker ut posteringene som gjelder tiltakspenger, med perioden oppdragssystemet stemplet dem med.
 *
 * Vi splitter dem bevisst ikke opp i dager her.
 * For ytelsesposteringer ville en dagsplitt vært eksakt, siden vi selv sender hele kroner per dag og OS slår sammen sammenhengende dager med samme dagsbeløp.
 * For motregning -- FEILUTBETALING, MOTPOSTERING, JUSTERING og TREKK -- ville den vært en tilnærming, fordi beløpet ofte bare dekker en del av perioden det er stemplet med.
 * Et uttrekk fra dev viste for eksempel en reduksjon på fire like dager splittet i én justering på tre dagers verdi og én feilutbetaling på én dags verdi, begge stemplet med hele firedagersperioden.
 *
 * Dagsverdier utledes derfor først i [OppsummeringGenerator], og kun til visning.
 * Se [Postering.beløpPerDag].
 */
private fun SimuleringResponseDTO.tilPosteringer(): List<Postering> {
    return this.detaljer.perioder.flatMap { posteringerForPeriode ->
        posteringerForPeriode.posteringer.mapNotNull { postering ->
            if (postering.fagområde != "TILTAKSPENGER") {
                // Fjerner alle posteringer som ikke er tiltakspenger.
                return@mapNotNull null
            }
            Postering(
                periode = postering.periode,
                fagområde = postering.fagområde,
                beløp = postering.beløp,
                type = postering.typeToDomain(),
                klassekode = postering.klassekode,
            )
        }
    }
}
