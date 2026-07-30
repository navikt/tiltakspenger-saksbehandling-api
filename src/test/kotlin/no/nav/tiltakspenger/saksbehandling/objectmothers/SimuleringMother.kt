package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlignBeregninger
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Klassekoder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Postering
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
import java.time.YearMonth
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
            ),
        ),
        posteringer: NonEmptyList<Postering> = nonEmptyListOf(
            Postering(
                periode = Periode(periode.fraOgMed, periode.fraOgMed),
                fagområde = "TILTAKSPENGER",
                beløp = 0,
                type = Posteringstype.YTELSE,
                klassekode = "test_klassekode",
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
                    posteringer = posteringer,
                ),
            ),
            simuleringstidspunkt = simuleringstidspunkt,
        )
    }
}

/**
 * Simuleringsscenarioer som ikke kan oppstå av vår egen beregning, trigget av faste test-fødselsnumre.
 * OS lager dem ved motregning mot forhold utenfor behandlingen -- kravgrunnlag, namsmann -- så lokalt må de bestilles eksplisitt.
 * Opprett saken med scenarioets fødselsnummer, så får alle simuleringene på saken scenarioets oppførsel.
 * Personen gjenbrukes mellom kjøringer lokalt -- endres scenariooppsettet (f.eks. innvilgelsesperioden), må fødselsnummeret byttes eller databasen resettes.
 * Scriptene i tiltakspenger-saksbehandling `scripts/testdata/` gjør dette for deg.
 */
enum class DevSimuleringsscenario(val fnr: String) {
    /**
     * Reduserte dager blir justering uten motpost i behandlingen, som når oppdrag motregner mot meldeperioder utenfor simuleringen.
     * Justeringen balanserer dermed ikke, og iverksetting sperres.
     */
    UBALANSERT_JUSTERING("99999999901"),

    /** Trekk fra kreditor over flere dager, pluss en reversering av et tidligere trekk på siste endrede dag. */
    TREKK("99999999902"),

    /**
     * Gjenskaper dev-casen i `TrekkMedJusteringFraDevTest`: tre rene førstegangsutbetalinger, der skattetrekket (~10 % per uke) omfordeles når tredje meldeperiode krysser et månedsskifte.
     * Alle simuleringene på saken får skattetrekk per hverdagsuke.
     * Når behandlingen krysser et månedsskifte og det finnes en forrige meldeperiode, omfordeles i tillegg forrige måneds trekk (begge fortegn) med justeringer som motpost -- balansert i måneden, på tvers av meldeperiodene.
     * Tillates med advarsel; det er nettopp denne casen vernet ble myknet for.
     */
    TREKK_MED_JUSTERING("99999999913"),

    /**
     * Spiller av de tre innspilte responsene fra dev-casen i `TrekkMedJusteringFraDevTest` uendret, valgt på behandlingens meldeperiode.
     * Krever innvilgelse 04.06.2026--12.07.2026, slik at meldeperiodene blir 01.06--14.06, 15.06--28.06 og 29.06--12.07.
     * Behandlinger utenfor de tre meldeperiodene faller tilbake til vanlig generering.
     */
    TREKK_MED_JUSTERING_EKSAKT("99999999906"),

    /**
     * Ytelse flyttet mellom meldeperioder: økningen i behandlingen motregnes mot en reversert dag i forrige meldeperiode.
     * Justeringene balanserer i kalendermåneden uten feilutbetaling, men den reverserte ytelsen gjør at vernet fortsatt sperrer.
     * Krever en forrige meldeperiode med dager i samme kalendermåned som økningen.
     */
    YTELSE_FLYTTET_MELLOM_MELDEPERIODER("99999999905"),

    /**
     * Justering som balanserer innenfor meldeperioden, men krysser kalendermåneden -- flytt en dag fra siste del av én måned til starten av neste.
     * Oppdrag gjør ikke dette i dag (splitter på månedsskiftet), så scenarioet finnes for å se månedsgrupperingen i vernet.
     * Krever en meldeperiode som spenner et månedsskifte, og en korrigering som flytter beløp mellom månedene.
     */
    JUSTERING_OVER_MÅNEDSSKIFTE("99999999904"),
    ;

    companion object {
        fun fraFnr(fnr: Fnr): DevSimuleringsscenario? =
            entries.firstOrNull { it.fnr == fnr.verdi }
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
 * - Omfordeling -- reduksjoner og økninger som går opp i null innenfor samme meldeperiode og kalendermåned -- blir JUSTERING i stedet for feilutbetaling og etterbetaling, slik oppdrag svarer når beløp bare er flyttet mellom dager (jf. `TrekkOgJusteringFraProdTest`).
 *
 * Ytelsesposteringene lages med én periode per dag, slik OS gjør for dagytelser -- da fordeles og avrundes ingen beløp.
 * Justeringene lages derimot som én postering per sammenhengende kjøring av dager, slik OS periodiserer dem.
 * Dermed får lokal kjøring og e2e-testene også dekket flerdagsposteringer uten dagsandel.
 *
 * Trekk oppstår hos OS (namsmann, kreditorer), ikke av vår egen beregning, og genereres derfor kun i [DevSimuleringsscenario.TREKK].
 * Se `OppsummeringGeneratorTrekkTest` for dekning av utregningen.
 */
fun Sak.genererSimuleringFraBeregning(
    beregning: Beregning,
    meldeperiodeKjeder: MeldeperiodeKjeder = this.meldeperiodeKjeder,
    clock: Clock = fixedClock,
    simuleringstidspunkt: LocalDateTime = nå(clock),
    scenario: DevSimuleringsscenario? = null,
): SimuleringMedMetadata {
    if (scenario == DevSimuleringsscenario.TREKK_MED_JUSTERING_EKSAKT) {
        eksaktTrekkMedJusteringRespons(
            meldeperiodeFraOgMed = beregning.beregninger.first().kjedeId.fraOgMed,
            gjelderId = this.fnr.verdi,
            sakId = this.saksnummer.verdi,
        )?.let { responsJson ->
            return tilSimuleringMedMetadata(responsJson, meldeperiodeKjeder, clock, simuleringstidspunkt)
        }
    }

    val simuleringsdager = mutableListOf<HelvedSimuleringsdag>()
    val justeringsposteringer = mutableListOf<HelvedJusteringspostering>()
    val trekkposteringer = mutableListOf<HelvedTrekkpostering>()
    val ekstraposteringer = mutableListOf<HelvedEkstrapostering>()

    beregning.beregninger.toList().forEach { beregningEtter ->
        val beregningFør = this.meldeperiodeBeregninger.hentForrigeBeregningForSimulering(beregningEtter)
        val endredeDager = sammenlignBeregninger(
            forrigeBeregning = beregningFør,
            gjeldendeBeregning = beregningEtter,
        ).dager.filter { it.erEndret }

        // Justering over månedsskiftet finnes ikke hos oppdrag; scenarioet hopper derfor over månedsgrupperingen med vilje.
        val grupper = if (scenario == DevSimuleringsscenario.JUSTERING_OVER_MÅNEDSSKIFTE) {
            listOf(endredeDager)
        } else {
            endredeDager.groupBy { it.dato.month }.values.toList()
        }

        grupper.forEach { dagerForMåned ->
            val erOmfordeling = scenario == DevSimuleringsscenario.UBALANSERT_JUSTERING ||
                (
                    dagerForMåned.any { it.totalbeløpEndring < 0 } &&
                        dagerForMåned.any { it.totalbeløpEndring > 0 } &&
                        dagerForMåned.sumOf { it.totalbeløpEndring } == 0
                    )

            simuleringsdager += dagerForMåned.map { dag ->
                HelvedSimuleringsdag(
                    dato = dag.dato,
                    tidligereUtbetalt = dag.forrigeTotalbeløp,
                    nyttBeløp = max(dag.nyttTotalbeløp, 0),
                    erJustert = erOmfordeling && dag.totalbeløpEndring < 0,
                )
            }
            if (erOmfordeling) {
                // I ubalansert-scenarioet finnes bare reduksjonene her -- motposten ligger i meldeperioder utenfor behandlingen.
                val dagerMedJustering = if (scenario == DevSimuleringsscenario.UBALANSERT_JUSTERING) {
                    dagerForMåned.filter { it.totalbeløpEndring < 0 }
                } else {
                    dagerForMåned
                }
                justeringsposteringer += dagerMedJustering.tilJusteringsposteringer()
            }
        }

        if (scenario == DevSimuleringsscenario.TREKK_MED_JUSTERING && endredeDager.isNotEmpty()) {
            leggSkattetrekkOgEventuellOmfordeling(
                endredeDager = endredeDager,
                meldeperiodeKjeder = meldeperiodeKjeder,
                justeringsposteringer = justeringsposteringer,
                trekkposteringer = trekkposteringer,
            )
        }

        if (scenario == DevSimuleringsscenario.YTELSE_FLYTTET_MELLOM_MELDEPERIODER && endredeDager.isNotEmpty()) {
            leggFlyttetYtelseFraForrigeMeldeperiode(
                endredeDager = endredeDager,
                meldeperiodeKjeder = meldeperiodeKjeder,
                justeringsposteringer = justeringsposteringer,
                ekstraposteringer = ekstraposteringer,
            )
        }

        if (scenario == DevSimuleringsscenario.TREKK && trekkposteringer.isEmpty() && endredeDager.isNotEmpty()) {
            val førsteKjøring = endredeDager.delISammenhengendeKjøringer().first().take(3)
            trekkposteringer += HelvedTrekkpostering(
                fom = førsteKjøring.first().dato,
                tom = førsteKjøring.last().dato,
                beløp = -191,
                klassekode = Klassekoder.TREKK_KREDITOR,
            )
            trekkposteringer += HelvedTrekkpostering(
                fom = endredeDager.last().dato,
                tom = endredeDager.last().dato,
                beløp = 50,
                klassekode = Klassekoder.TREKK_BIDRAG,
            )
        }
    }

    val responsJson = byggHelvedSimuleringRespons(
        gjelderId = this.fnr.verdi,
        sakId = this.saksnummer.verdi,
        datoBeregnet = LocalDate.now(clock),
        dager = simuleringsdager.sortedBy { it.dato },
        justeringsposteringer = justeringsposteringer,
        trekkposteringer = trekkposteringer,
        ekstraposteringer = ekstraposteringer,
    )

    return tilSimuleringMedMetadata(responsJson, meldeperiodeKjeder, clock, simuleringstidspunkt)
}

private fun tilSimuleringMedMetadata(
    responsJson: String,
    meldeperiodeKjeder: MeldeperiodeKjeder,
    clock: Clock,
    simuleringstidspunkt: LocalDateTime,
): SimuleringMedMetadata {
    val simulering = deserialize<SimuleringResponseDTO>(responsJson)
        .toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock)
        .getOrElse { feil -> throw AssertionError("Simuleringsfaken bygde en respons som ikke kan tolkes: ${feil.loggkontekst.melding}") }

    return SimuleringMedMetadata(
        simulering = when (simulering) {
            is Simulering.Endring -> simulering.copy(simuleringstidspunkt = simuleringstidspunkt)
            is Simulering.IngenEndring -> simulering.copy(simuleringstidspunkt = simuleringstidspunkt)
        },
        originalResponseBody = responsJson,
    )
}

/**
 * De tre innspilte responsene fra dev-casen i `TrekkMedJusteringFraDevTest`, én per meldeperiode.
 * Posteringene er gjengitt uendret; fødselsnummer og saksnummer settes inn fra saken.
 * `oppsummeringer` utelates fordi mapperen ikke leser dem -- aggregatene regnes ut av domenekoden.
 */
private fun eksaktTrekkMedJusteringRespons(
    meldeperiodeFraOgMed: LocalDate,
    gjelderId: String,
    sakId: String,
): String? {
    //language=json
    val perMeldeperiode = mapOf(
        LocalDate.of(2026, 6, 1) to """
        {
          "oppsummeringer": [],
          "detaljer": {
            "gjelderId": "$gjelderId",
            "datoBeregnet": "2026-06-22",
            "totalBeløp": 1966,
            "perioder": [
              {"fom": "2026-06-04", "tom": "2026-06-05", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-04", "tom": "2026-06-05", "beløp": 624, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-04", "tom": "2026-06-05", "beløp": -62, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-06-08", "tom": "2026-06-12", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-08", "tom": "2026-06-12", "beløp": 1560, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-08", "tom": "2026-06-12", "beløp": -156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]}
            ]
          }
        }
        """.trimIndent(),

        LocalDate.of(2026, 6, 15) to """
        {
          "oppsummeringer": [],
          "detaljer": {
            "gjelderId": "$gjelderId",
            "datoBeregnet": "2026-06-29",
            "totalBeløp": 2808,
            "perioder": [
              {"fom": "2026-06-15", "tom": "2026-06-19", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": 1560, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": -156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-06-22", "tom": "2026-06-26", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": 1560, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": -156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]}
            ]
          }
        }
        """.trimIndent(),

        LocalDate.of(2026, 6, 29) to """
        {
          "oppsummeringer": [],
          "detaljer": {
            "gjelderId": "$gjelderId",
            "datoBeregnet": "2026-07-24",
            "totalBeløp": 2808,
            "perioder": [
              {"fom": "2026-06-15", "tom": "2026-06-19", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": 81, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": -237, "type": "TREKK", "klassekode": "PSKTSKAT"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": 156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-06-22", "tom": "2026-06-26", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": -58, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": -98, "type": "TREKK", "klassekode": "PSKTSKAT"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": 156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-06-29", "tom": "2026-06-30", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": -23, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": 624, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": -39, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-07-01", "tom": "2026-07-03", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-07-01", "tom": "2026-07-03", "beløp": 936, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-07-01", "tom": "2026-07-03", "beløp": -94, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]},
              {"fom": "2026-07-06", "tom": "2026-07-10", "posteringer": [
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-07-06", "tom": "2026-07-10", "beløp": 1560, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
                {"fagområde": "TILTAKSPENGER", "sakId": "$sakId", "fom": "2026-07-06", "tom": "2026-07-10", "beløp": -156, "type": "TREKK", "klassekode": "PSKTSKAT"}
              ]}
            ]
          }
        }
        """.trimIndent(),
    )
    return perMeldeperiode[meldeperiodeFraOgMed]
}

internal data class HelvedSimuleringsdag(
    val dato: LocalDate,
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,

    /**
     * Dagen er redusert som ledd i en omfordeling som går opp i null innenfor måneden.
     * Da dekkes reduksjonen av en justeringspostering, og dagen skal ikke ha feilutbetaling og motpostering.
     */
    val erJustert: Boolean = false,
)

/** En justeringspostering slik OS periodiserer dem: ett beløp for en periode som kan spenne flere dager. */
internal data class HelvedJusteringspostering(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
)

/** En vilkårlig postering med egen type og klassekode, for scenarioer som trenger mer enn ytelse, justering og trekk. */
internal data class HelvedEkstrapostering(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val type: String,
    val klassekode: String,
)

/**
 * Et trekk slik OS leverer dem: ett beløp for en periode, negativt for nye trekk og positivt for reversering av tidligere trekk.
 * Genereres kun i [DevSimuleringsscenario.TREKK] -- trekk oppstår hos OS, ikke av vår beregning.
 */
internal data class HelvedTrekkpostering(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val klassekode: String,
)

/**
 * Økningen i behandlingen motregnes mot en dag som reverseres i forrige meldeperiode -- ytelse flyttet mellom meldeperioder.
 * Justeringene balanserer i måneden, men den reverserte ytelsen i forrige meldeperiode skal få vernet til å sperre.
 */
private fun leggFlyttetYtelseFraForrigeMeldeperiode(
    endredeDager: List<SammenligningAvBeregninger.DagSammenligning>,
    meldeperiodeKjeder: MeldeperiodeKjeder,
    justeringsposteringer: MutableList<HelvedJusteringspostering>,
    ekstraposteringer: MutableList<HelvedEkstrapostering>,
) {
    // Kun dager som var beregnet før -- en førstegangsutbetaling er også «økning», men den motregnes ikke mot noe.
    val økte = endredeDager.filter { it.totalbeløpEndring > 0 && it.status.forrige != null }
    if (økte.isEmpty()) return
    val sum = økte.sumOf { it.totalbeløpEndring }
    val måned = YearMonth.from(økte.first().dato)

    val forrigeMeldeperiode = meldeperiodeKjeder
        .flatMap { kjede -> kjede.map { it.periode } }
        .filter { it.tilOgMed < endredeDager.first().dato }
        .maxByOrNull { it.tilOgMed }
        ?: return
    val reversertDag = generateSequence(forrigeMeldeperiode.tilOgMed) { it.minusDays(1) }
        .takeWhile { it >= forrigeMeldeperiode.fraOgMed }
        .firstOrNull { it.dayOfWeek.value <= 5 && YearMonth.from(it) == måned }
        ?: return

    // Dagen i forrige meldeperiode reverseres, og justeringen dekker den i stedet for feilutbetaling.
    ekstraposteringer += HelvedEkstrapostering(reversertDag, reversertDag, -sum, "YTELSE", "TPTPOPPFAG")
    justeringsposteringer += HelvedJusteringspostering(reversertDag, reversertDag, sum)
    // Økningen i behandlingen dekkes av motregningen i stedet for å bli etterbetaling.
    justeringsposteringer += HelvedJusteringspostering(økte.first().dato, økte.last().dato, -sum)
}

/**
 * Skattetrekk og eventuell omfordeling, slik dev-casen i `TrekkMedJusteringFraDevTest` så ut.
 *
 * Alle behandlingene får forskuddstrekk på ~10 % av ytelsen per sammenhengende kjøring av endrede dager -- det matcher −62/−156-posteringene i casen.
 * Krysser behandlingen et månedsskifte og det finnes en forrige meldeperiode, omfordeles i tillegg forrige måneds trekk med casens tall:
 * trekkene i forrige meldeperiode korrigeres (−237/+156 og −98/+156 per hverdagsuke) med justeringer +81/−58 som motpost, og behandlingens del av samme måned får justeringen −23.
 * Justeringene summerer til null i måneden, så vernet tillater dem med advarsel.
 */
private fun leggSkattetrekkOgEventuellOmfordeling(
    endredeDager: List<SammenligningAvBeregninger.DagSammenligning>,
    meldeperiodeKjeder: MeldeperiodeKjeder,
    justeringsposteringer: MutableList<HelvedJusteringspostering>,
    trekkposteringer: MutableList<HelvedTrekkpostering>,
) {
    // Ved en førstegangsbehandling er alle dager i meldeperioden «endret», også helger og dager uten beløp.
    // Trekkene i dev-casen følger hverdagsukene med ytelse, så dager uten beløp filtreres vekk før kjøringene deles opp.
    val dagerMedYtelse = endredeDager.filter { it.nyttTotalbeløp > 0 }
    if (dagerMedYtelse.isEmpty()) {
        return
    }

    // Oppdrag deler aldri en postering over et månedsskifte, jf. dev-casen der 29.06–30.06 og 01.07–03.07 er egne trekkposteringer.
    val kjøringer = dagerMedYtelse.delISammenhengendeKjøringer()
        .flatMap { kjøring -> kjøring.groupBy { YearMonth.from(it.dato) }.values }
    kjøringer.forEach { kjøring ->
        val ytelse = kjøring.sumOf { it.nyttTotalbeløp }
        if (ytelse > 0) {
            trekkposteringer += HelvedTrekkpostering(
                fom = kjøring.first().dato,
                tom = kjøring.last().dato,
                beløp = -(ytelse / 10),
                klassekode = Klassekoder.TREKK_SKATT,
            )
        }
    }

    val førsteMåned = YearMonth.from(dagerMedYtelse.first().dato)
    val krysserMånedsskifte = YearMonth.from(dagerMedYtelse.last().dato) != førsteMåned
    val forrigeMeldeperiode = meldeperiodeKjeder
        .flatMap { kjede -> kjede.map { it.periode } }
        .filter { it.tilOgMed < dagerMedYtelse.first().dato }
        .maxByOrNull { it.tilOgMed }
    if (!krysserMånedsskifte || forrigeMeldeperiode == null) {
        return
    }

    // Hverdagsukene i forrige meldeperiode, som i casen fikk trekkene sine korrigert.
    val forrigeUker = generateSequence(forrigeMeldeperiode.fraOgMed) { it.plusDays(1) }
        .takeWhile { it <= forrigeMeldeperiode.tilOgMed }
        .filter { it.dayOfWeek.value <= 5 && YearMonth.from(it) == førsteMåned }
        .toList()
        .fold(mutableListOf<MutableList<LocalDate>>()) { uker, dag ->
            val siste = uker.lastOrNull()
            if (siste != null && siste.last().plusDays(1) == dag) siste.add(dag) else uker.add(mutableListOf(dag))
            uker
        }
        .take(2)
    if (forrigeUker.size < 2) {
        return
    }

    val (uke1, uke2) = forrigeUker
    trekkposteringer += HelvedTrekkpostering(uke1.first(), uke1.last(), -237, Klassekoder.TREKK_SKATT)
    trekkposteringer += HelvedTrekkpostering(uke1.first(), uke1.last(), 156, Klassekoder.TREKK_SKATT)
    trekkposteringer += HelvedTrekkpostering(uke2.first(), uke2.last(), -98, Klassekoder.TREKK_SKATT)
    trekkposteringer += HelvedTrekkpostering(uke2.first(), uke2.last(), 156, Klassekoder.TREKK_SKATT)
    justeringsposteringer += HelvedJusteringspostering(uke1.first(), uke1.last(), 81)
    justeringsposteringer += HelvedJusteringspostering(uke2.first(), uke2.last(), -58)

    val behandlingensDelAvMåneden = dagerMedYtelse.filter { YearMonth.from(it.dato) == førsteMåned }
    justeringsposteringer += HelvedJusteringspostering(
        fom = behandlingensDelAvMåneden.first().dato,
        tom = behandlingensDelAvMåneden.last().dato,
        beløp = -23,
    )
}

/**
 * Oversetter en balansert omfordeling til justeringsposteringer slik oppdrag gjør det.
 *
 * Reduserte dager får positiv justering (tidligere utbetalt dekkes av justeringen i stedet for å bli feilutbetaling), økte dager får negativ (beløpet dekkes av omposteringen i stedet for å bli etterbetaling).
 * Sammenhengende dager slås sammen til én postering, slik at flerdagsposteringer uten dagsandel også oppstår i testdataene.
 */
private fun List<SammenligningAvBeregninger.DagSammenligning>.tilJusteringsposteringer(): List<HelvedJusteringspostering> {
    return listOf(
        filter { it.totalbeløpEndring < 0 },
        filter { it.totalbeløpEndring > 0 },
    ).flatMap { dagerMedSammeFortegn ->
        dagerMedSammeFortegn
            .sortedBy { it.dato }
            .delISammenhengendeKjøringer()
            .map { kjøring ->
                HelvedJusteringspostering(
                    fom = kjøring.first().dato,
                    tom = kjøring.last().dato,
                    beløp = -kjøring.sumOf { it.totalbeløpEndring },
                )
            }
    }
}

private fun List<SammenligningAvBeregninger.DagSammenligning>.delISammenhengendeKjøringer(): List<List<SammenligningAvBeregninger.DagSammenligning>> {
    return fold(mutableListOf<MutableList<SammenligningAvBeregninger.DagSammenligning>>()) { kjøringer, dag ->
        val siste = kjøringer.lastOrNull()
        if (siste != null && siste.last().dato.plusDays(1) == dag.dato) {
            siste.add(dag)
        } else {
            kjøringer.add(mutableListOf(dag))
        }
        kjøringer
    }
}

/**
 * Klassekoder hentet fra ekte responser, slik at testdataene ligner på det vi faktisk får.
 *
 * Feilutbetalings-, justerings- og motposteringskoden hentes fra produksjonskoden og [Klassekoder], ikke duplikeres her.
 * Ellers ville faken kunne drive fra hverandre med koden den skal etterligne.
 */
private const val KLASSEKODE_YTELSE = "TPTPAFT"
private val KLASSEKODE_FEILUTBETALING = OppsummeringGenerator.KLASSEKODE_FEILUTBETALING
private val KLASSEKODE_JUSTERING = OppsummeringGenerator.KLASSEKODE_JUSTERING
private val KLASSEKODE_MOTPOSTERING = Klassekoder.MOTPOSTERING

internal fun byggHelvedSimuleringRespons(
    gjelderId: String,
    sakId: String,
    datoBeregnet: LocalDate,
    dager: List<HelvedSimuleringsdag>,
    justeringsposteringer: List<HelvedJusteringspostering> = emptyList(),
    trekkposteringer: List<HelvedTrekkpostering> = emptyList(),
    ekstraposteringer: List<HelvedEkstrapostering> = emptyList(),
): String {
    val dagPerioder = dager.mapNotNull { dag ->
        val feilutbetaling = if (dag.erJustert) 0 else max(dag.tidligereUtbetalt - dag.nyttBeløp, 0)
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
        dag.dato to """{"fom":"${dag.dato}","tom":"${dag.dato}","posteringer":[$posteringerJson]}"""
    }

    val justeringsperioder = justeringsposteringer.map { justering ->
        justering.fom to """
        {"fom":"${justering.fom}","tom":"${justering.tom}","posteringer":[
         {"fagområde":"TILTAKSPENGER","sakId":"$sakId","fom":"${justering.fom}","tom":"${justering.tom}",
          "beløp":${justering.beløp},"type":"JUSTERING","klassekode":"$KLASSEKODE_JUSTERING"}]}
        """.trimIndent()
    }

    val trekkperioder = trekkposteringer.map { trekk ->
        trekk.fom to """
        {"fom":"${trekk.fom}","tom":"${trekk.tom}","posteringer":[
         {"fagområde":"TILTAKSPENGER","sakId":"$sakId","fom":"${trekk.fom}","tom":"${trekk.tom}",
          "beløp":${trekk.beløp},"type":"TREKK","klassekode":"${trekk.klassekode}"}]}
        """.trimIndent()
    }

    val ekstraperioder = ekstraposteringer.map { ekstra ->
        ekstra.fom to """
        {"fom":"${ekstra.fom}","tom":"${ekstra.tom}","posteringer":[
         {"fagområde":"TILTAKSPENGER","sakId":"$sakId","fom":"${ekstra.fom}","tom":"${ekstra.tom}",
          "beløp":${ekstra.beløp},"type":"${ekstra.type}","klassekode":"${ekstra.klassekode}"}]}
        """.trimIndent()
    }

    val perioder = (dagPerioder + justeringsperioder + trekkperioder + ekstraperioder)
        .sortedBy { (fom, _) -> fom }
        .map { (_, json) -> json }

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
