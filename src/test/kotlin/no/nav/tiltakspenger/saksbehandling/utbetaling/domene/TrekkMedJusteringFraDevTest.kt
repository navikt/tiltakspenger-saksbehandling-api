package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.SimuleringResponseDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toSimuleringFraHelvedResponse
import org.junit.jupiter.api.Test
import java.time.YearMonth

/**
 * Reell case fra dev 2026-07-24: forskuddstrekk skatt (PSKTSKAT) med begge fortegn i samme periode, og justeringer som balanserer i kalendermåneden men krysser meldeperiodegrensen.
 *
 * Justeringene er +81 og −58 i meldeperioden 15.06–28.06 og −23 i meldeperioden 29.06–12.07 -- sum null i juni, men +23/−23 per meldeperiode.
 * Casen stoppet en brukers sak: bare førstegangsutbetalinger, ingen korrigering, men oppdrag omfordeler forskuddstrekket per måned med justeringer som motpost.
 * Slike justeringer skal tillates med advarsel -- de balanserer i kalendermåneden og har ingen feilutbetaling, så månedens utbetaling til bruker er uendret.
 *
 * Responsen er en anonymisert gjengivelse av det reelle svaret; fødselsnummer og saksnummer er byttet ut med testverdier.
 */
class TrekkMedJusteringFraDevTest {

    private val fnr = Fnr.random()
    private val sakId = SakId.random()
    private val saksnummer = Saksnummer.genererSaknummer(løpenr = "1042", clock = fixedClock)

    private val meldeperiodeKjeder = MeldeperiodeKjeder(
        listOf(
            Periode(15.juni(2026), 28.juni(2026)),
            Periode(29.juni(2026), 12.juli(2026)),
        ).map { periode ->
            MeldeperiodeKjede(
                ObjectMother.meldeperiode(
                    periode = periode,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                ),
            )
        },
    )

    //language=json
    private fun responsJson(): String = """
    {
      "oppsummeringer": [
        {"fom": "2026-06-15", "tom": "2026-06-30", "tidligereUtbetalt": 0, "nyUtbetaling": 624, "totalEtterbetaling": 624, "totalFeilutbetaling": 0},
        {"fom": "2026-07-01", "tom": "2026-07-10", "tidligereUtbetalt": 0, "nyUtbetaling": 2496, "totalEtterbetaling": 2496, "totalFeilutbetaling": 0}
      ],
      "detaljer": {
        "gjelderId": "${fnr.verdi}",
        "datoBeregnet": "2026-07-24",
        "totalBeløp": 2808,
        "perioder": [
          {"fom": "2026-06-15", "tom": "2026-06-19", "posteringer": [
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": 81, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": -237, "type": "TREKK", "klassekode": "PSKTSKAT"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-15", "tom": "2026-06-19", "beløp": 156, "type": "TREKK", "klassekode": "PSKTSKAT"}
          ]},
          {"fom": "2026-06-22", "tom": "2026-06-26", "posteringer": [
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": -58, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": -98, "type": "TREKK", "klassekode": "PSKTSKAT"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-22", "tom": "2026-06-26", "beløp": 156, "type": "TREKK", "klassekode": "PSKTSKAT"}
          ]},
          {"fom": "2026-06-29", "tom": "2026-06-30", "posteringer": [
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": -23, "type": "JUSTERING", "klassekode": "KL_KODE_JUST_ARBYT"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": 624, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-06-29", "tom": "2026-06-30", "beløp": -39, "type": "TREKK", "klassekode": "PSKTSKAT"}
          ]},
          {"fom": "2026-07-01", "tom": "2026-07-03", "posteringer": [
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-07-01", "tom": "2026-07-03", "beløp": 936, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-07-01", "tom": "2026-07-03", "beløp": -94, "type": "TREKK", "klassekode": "PSKTSKAT"}
          ]},
          {"fom": "2026-07-06", "tom": "2026-07-10", "posteringer": [
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-07-06", "tom": "2026-07-10", "beløp": 1560, "type": "YTELSE", "klassekode": "TPTPOPPFAG"},
            {"fagområde": "TILTAKSPENGER", "sakId": "${saksnummer.verdi}", "fom": "2026-07-06", "tom": "2026-07-10", "beløp": -156, "type": "TREKK", "klassekode": "PSKTSKAT"}
          ]}
        ]
      }
    }
    """.trimIndent()

    @Test
    fun `casen tolkes, og trekkene summeres per meldeperiode uansett fortegn`() {
        val simulering = deserialize<SimuleringResponseDTO>(responsJson())
            .toSimuleringFraHelvedResponse(meldeperiodeKjeder, fixedClock)
            .forventTolkbar() as Simulering.Endring

        simulering.simuleringPerMeldeperiode.size shouldBe 2
        val (første, andre) = simulering.simuleringPerMeldeperiode.toList()

        // −237 + 156 − 98 + 156 = −23; reverseringene av tidligere trekk motregnes.
        første.totalTrekk shouldBe -23
        andre.totalTrekk shouldBe -39 - 94 - 156

        første.flagg.harTrekk shouldBe true
        første.flagg.harJustering shouldBe true
        andre.flagg.harTrekk shouldBe true
    }

    @Test
    fun `justeringene krysser meldeperiodene men balanserer i juni, og iverksetting tillates`() {
        val simulering = deserialize<SimuleringResponseDTO>(responsJson())
            .toSimuleringFraHelvedResponse(meldeperiodeKjeder, fixedClock)
            .forventTolkbar() as Simulering.Endring

        // Per meldeperiode er justeringene ubalanserte -- det er dette saksbehandler advares om i visningen.
        simulering.simuleringPerMeldeperiode.map { it.ubalanserteJusteringsmåneder } shouldBe listOf(
            mapOf(YearMonth.of(2026, 6) to 23),
            mapOf(YearMonth.of(2026, 6) to -23),
        )

        // Men på tvers av simuleringen går juni opp i null, og det finnes ingen feilutbetaling -- da er det trekkomfordeling, ikke motregning av ytelse.
        simulering.ubalanserteJusteringsmåneder shouldBe emptyMap()
        simulering.harFeilutbetaling shouldBe false

        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }
}
