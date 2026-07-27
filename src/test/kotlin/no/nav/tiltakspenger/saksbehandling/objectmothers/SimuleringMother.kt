package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlignBeregninger
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Klassekoder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.hentForrigeBeregningForSimulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.SimuleringResponseDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toSimuleringFraHelvedResponse
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.max

interface SimuleringMother {

    fun simuleringMedMetadata(
        simulering: Simulering = simulering(),
        originalJson: String = "{}",
    ): SimuleringMedMetadata {
        return SimuleringMedMetadata(
            simulering = simulering,
            originalResponseBody = originalJson,
        )
    }

    fun simulering(
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        meldeperiodeKjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        meldeperiode: Meldeperiode = ObjectMother.meldeperiode(
            periode = periode,
            kjedeId = meldeperiodeKjedeId,
        ),
        simuleringsdager: NonEmptyList<Simuleringsdag> = nonEmptyListOf(
            Simuleringsdag(
                dato = periode.fraOgMed,
                tidligereUtbetalt = 0,
                nyUtbetaling = 0,
                totalEtterbetaling = 0,
                totalFeilutbetaling = 0,
                totalTrekk = 0,
                totalJustering = 0,
                totalMotpostering = 0,
                harJustering = false,
                posteringsdag = PosteringerForDag(
                    dato = periode.fraOgMed,
                    posteringer = nonEmptyListOf(
                        PosteringForDag(
                            dato = periode.fraOgMed,
                            fagområde = "TILTAKSPENGER",
                            beløp = 0,
                            type = Posteringstype.YTELSE,
                            klassekode = "test_klassekode",
                        ),
                    ),
                ),
            ),
        ),
        clock: Clock = fixedClock,
        simuleringstidspunkt: LocalDateTime = nå(clock),
    ): Simulering.Endring {
        return Simulering.Endring(
            datoBeregnet = periode.tilOgMed,
            totalBeløp = 0,
            simuleringPerMeldeperiode = nonEmptyListOf(
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    simuleringsdager = simuleringsdager,
                ),
            ),
            simuleringstidspunkt = simuleringstidspunkt,
        )
    }
}

/**
 * Ment brukt både når man kjører lokalt og i testene for å lage litt mer realistiske testdata.
 */
fun Sak.genererSimuleringFraMeldekortbehandling(
    behandling: Meldekortbehandling,
    meldeperiodeKjeder: MeldeperiodeKjeder = this.meldeperiodeKjeder,
): SimuleringMedMetadata {
    return genererSimuleringFraBeregning(
        beregning = behandling.beregning!!,
        meldeperiodeKjeder = meldeperiodeKjeder,
    )
}

/**
 * Ment brukt både når man kjører lokalt og i testene for å lage litt mer realistiske testdata.
 *
 * Bygger responsen slik helved ville sendt den, og kjører den gjennom den ekte mapperen.
 * Da får både lokal kjøring og e2e-testene med seg deserialiseringen, fagområdefiltreringen og [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator] -- i stedet for at denne funksjonen regner ut aggregatene selv og domenekoden aldri blir kjørt.
 *
 * Posteringsmønsteret følger helveds egen dokumentasjon
 * (https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md):
 *
 * - Ny utbetaling: én positiv ytelsespostering.
 * - Økning: positiv postering for det nye beløpet og negativ for det tidligere utbetalte.
 * - Reduksjon: i tillegg en positiv ytelsespostering og en FEILUTBETALING på differansen, med tilhørende negativ MOTPOSTERING, slik at ytelsesposteringene summerer til null.
 *
 * Vi lager én periode per dag, slik OS gjør for dagytelser.
 * Det unngår også at beløpene fordeles og avrundes per dag.
 *
 * Trekk og justering genereres ikke -- de oppsto ikke av vår egen beregning, men av forhold hos OS.
 * Se `OppsummeringGeneratorTrekkTest` og `OppsummeringGeneratorJusteringTest` for dekning av dem.
 */
fun Sak.genererSimuleringFraBeregning(
    beregning: Beregning,
    meldeperiodeKjeder: MeldeperiodeKjeder = this.meldeperiodeKjeder,
    clock: Clock = fixedClock,
    simuleringstidspunkt: LocalDateTime = nå(clock),
): SimuleringMedMetadata {
    val endredeDager = beregning.beregninger.toList().flatMap { beregningEtter ->
        val beregningFør = this.meldeperiodeBeregninger.hentForrigeBeregningForSimulering(beregningEtter)
        sammenlignBeregninger(
            forrigeBeregning = beregningFør,
            gjeldendeBeregning = beregningEtter,
        ).dager.filter { it.erEndret }
    }.sortedBy { it.dato }

    val responsJson = byggHelvedSimuleringRespons(
        gjelderId = this.fnr.verdi,
        sakId = this.saksnummer.verdi,
        datoBeregnet = LocalDate.now(clock),
        dager = endredeDager.map { dag ->
            HelvedSimuleringsdag(
                dato = dag.dato,
                tidligereUtbetalt = dag.forrigeTotalbeløp,
                nyttBeløp = max(dag.nyttTotalbeløp, 0),
            )
        },
    )

    val simulering = deserialize<SimuleringResponseDTO>(responsJson)
        .toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock)

    return SimuleringMedMetadata(
        simulering = when (simulering) {
            is Simulering.Endring -> simulering.copy(simuleringstidspunkt = simuleringstidspunkt)
            is Simulering.IngenEndring -> simulering.copy(simuleringstidspunkt = simuleringstidspunkt)
        },
        originalResponseBody = responsJson,
    )
}

internal data class HelvedSimuleringsdag(
    val dato: LocalDate,
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,
)

/**
 * Klassekoder hentet fra ekte responser, slik at testdataene ligner på det vi faktisk får.
 *
 * Feilutbetalings- og motposteringskoden hentes fra produksjonskoden og [Klassekoder], ikke duplikeres her.
 * Ellers ville faken kunne drive fra hverandre med koden den skal etterligne.
 */
private const val KLASSEKODE_YTELSE = "TPTPAFT"
private val KLASSEKODE_FEILUTBETALING = OppsummeringGenerator.KLASSEKODE_FEILUTBETALING
private val KLASSEKODE_MOTPOSTERING = Klassekoder.MOTPOSTERING

internal fun byggHelvedSimuleringRespons(
    gjelderId: String,
    sakId: String,
    datoBeregnet: LocalDate,
    dager: List<HelvedSimuleringsdag>,
): String {
    val perioder = dager.mapNotNull { dag ->
        val feilutbetaling = max(dag.tidligereUtbetalt - dag.nyttBeløp, 0)
        val posteringer = buildList {
            if (feilutbetaling > 0) add("YTELSE" to (feilutbetaling to KLASSEKODE_YTELSE))
            if (dag.nyttBeløp > 0) add("YTELSE" to (dag.nyttBeløp to KLASSEKODE_YTELSE))
            if (feilutbetaling > 0) {
                add("FEILUTBETALING" to (feilutbetaling to KLASSEKODE_FEILUTBETALING))
                add("MOTPOSTERING" to (-feilutbetaling to KLASSEKODE_MOTPOSTERING))
            }
            if (dag.tidligereUtbetalt > 0) {
                add("YTELSE" to (-dag.tidligereUtbetalt to KLASSEKODE_YTELSE))
            }
        }
        if (posteringer.isEmpty()) {
            return@mapNotNull null
        }
        val posteringerJson = posteringer.joinToString(",") { (type, beløpOgKlassekode) ->
            val (beløp, klassekode) = beløpOgKlassekode
            """
            {"fagområde":"TILTAKSPENGER","sakId":"$sakId","fom":"${dag.dato}","tom":"${dag.dato}",
             "beløp":$beløp,"type":"$type","klassekode":"$klassekode"}
            """.trimIndent()
        }
        """{"fom":"${dag.dato}","tom":"${dag.dato}","posteringer":[$posteringerJson]}"""
    }

    // //language=json
    return """
    {
      "oppsummeringer": [],
      "detaljer": {
        "gjelderId": "$gjelderId",
        "datoBeregnet": "$datoBeregnet",
        "totalBeløp": ${dager.sumOf { it.nyttBeløp }},
        "perioder": [${perioder.joinToString(",")}]
      }
    }
    """.trimIndent()
}
