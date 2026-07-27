package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.SimuleringResponseDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toSimuleringFraHelvedResponse
import java.time.LocalDate

/**
 * Kompakt testdata for simulering.
 *
 * Alternativet er JSON-blokker på hundre linjer per case, slik de fire opprinnelige testene i [OppsummeringGeneratorTest] er skrevet.
 * Her bygger vi responsen fra helved programmatisk i stedet, slik at en ny case blir én linje:
 *
 * ```
 * simulerDag(ytelse(408), ytelse(-112)).nyUtbetaling shouldBe 408
 * ```
 *
 * Vi går gjennom [toSimuleringFraHelvedResponse], ikke rett på [OppsummeringGenerator], slik at testene også dekker deserialiseringen, fagområdefiltreringen og fordelingen av beløp per dag.
 *
 * Kombinasjonene i testene er hentet fra et uttrekk av dev 2026-07-27:
 * 472 simuleringer kokte ned til 24 unike kombinasjoner av posteringstype, klassekode og fortegn.
 * TREKK og FORSKUDSSKATT forekom ikke i uttrekket og er derfor konstruert for hånd.
 */
internal object Simuleringstestdata {
    val fnr: Fnr = Fnr.random()
    val sakId: SakId = SakId.random()
    val saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = fixedClock)

    /** Mandag 6. januar til søndag 19. januar 2025. */
    val meldeperiode: Periode = 6 til 19.januar(2025)

    /**
     * Mandag 20. januar til søndag 2. februar 2025.
     * Brukes når en test trenger to meldeperioder, typisk for justeringer på tvers av dem.
     */
    val andreMeldeperiode: Periode = Periode(20.januar(2025), 2.februar(2025))

    val førsteDag: LocalDate = meldeperiode.fraOgMed

    val førsteDagIAndreMeldeperiode: LocalDate = andreMeldeperiode.fraOgMed

    val meldeperiodeKjeder: MeldeperiodeKjeder = MeldeperiodeKjeder(
        listOf(meldeperiode, andreMeldeperiode).map { periode ->
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
}

/**
 * Fagområdet vårt.
 * Posteringer med andre fagområder skal filtreres bort.
 */
const val FAGOMRÅDE_TILTAKSPENGER = "TILTAKSPENGER"

/**
 * Klassekodene som forekommer i dev-uttrekket, gruppert slik de opptrer.
 *
 * Ytelsesposteringene er det utbetalingen faktisk består av: de driver `tidligereUtbetalt`, `nyUtbetaling` og `totalEtterbetaling`, og de aller fleste dagene har ikke annet enn dem.
 * Feilutbetaling, justering og trekk er unntakene.
 *
 * Det som *ikke* betyr noe, er hvilken av ytelsesklassekodene som brukes -- de behandles helt likt.
 * Bare [OppsummeringGenerator.KLASSEKODE_FEILUTBETALING] og [OppsummeringGenerator.KLASSEKODE_JUSTERING] endrer utregningen ut fra klassekoden alene.
 * Vi tar vare på listen fordi den dokumenterer hva OS faktisk sender oss.
 */
object Klassekoder {
    /** Tiltakspenger, med barnetillegget som motstykke i [YTELSE_BARNETILLEGG]. */
    val YTELSE_TILTAKSPENGER = listOf(
        "TPTPAAG",
        "TPTPAFT",
        "TPTPATT",
        "TPTPGRVGSHOY",
        "TPTPIPS",
        "TPTPOPPFAG",
        "TPFORSAMOENK",
        "TPFORSAMOGRU",
        "TPFORSFAGENK",
        "TPFORSFAGGRU",
    )

    val YTELSE_BARNETILLEGG = listOf(
        "TPBTAAGR",
        "TPBTAF",
        "TPBTATTILT",
        "TPBTGRVGSHOY",
        "TPBTIPS",
        "TPBTOPPFAGR",
        "TPFORSAMOENKBT",
        "TPFORSAMOGRUBT",
        "TPFORSFAGENKBT",
        "TPFORSFAGGRUBT",
    )

    /** Alle 23 klassekodene vi har observert i dev. */
    val ALLE_OBSERVERTE = YTELSE_TILTAKSPENGER + YTELSE_BARNETILLEGG + listOf(
        OppsummeringGenerator.KLASSEKODE_FEILUTBETALING,
        OppsummeringGenerator.KLASSEKODE_JUSTERING,
        MOTPOSTERING,
    )

    const val MOTPOSTERING = "TBMOTOBS"

    /**
     * Trekk forekom ikke i dev-uttrekket, så disse er hentet fra ekte OS-responser i andre repoer:
     * `BSKTKRED` fra helved (`apps/simulering/test/sim-trekk.xml`) og `AVSUINTE` fra su-se-bakover (`simulering-dobbel-tilbakeføring-med-trekk.xml`).
     *
     * Klassekoden påvirker ikke utregningen; `beregnTrekk` filtrerer bare på posteringstype.
     * Fortegnet gjør derimot det -- se [OppsummeringGeneratorTrekkTest].
     */
    const val TREKK_BIDRAG = "BIDRINTE"

    const val TREKK_KREDITOR = "KREDKRED"

    const val TREKK_SKATT = "PSKTSKAT"

    const val TREKK_BARNEBIDRAG = "BSKTKRED"

    /** Alle sju trekk-klassekodene observert i prod, i synkende hyppighet. */
    val ALLE_TREKK = listOf(TREKK_KREDITOR, TREKK_SKATT, TREKK_BIDRAG, TREKK_BARNEBIDRAG, "FEISINTE", "GJELKRED", "TBTREKK")

    /**
     * Forskuddsskatt forekom ikke i dev.
     * Klassekoden er hentet fra su-se-bakover.
     */
    const val FORSKUDSSKATT = "FSKTSKAT"
}

/** En postering slik den kommer fra helved, før den splittes opp per dag. */
internal data class Testpostering(
    val type: String,
    val klassekode: String,
    val beløp: Int,
    val fagområde: String = FAGOMRÅDE_TILTAKSPENGER,
)

internal fun ytelse(beløp: Int, klassekode: String = "TPTPAFT") =
    Testpostering(type = "YTELSE", klassekode = klassekode, beløp = beløp)

/**
 * FEILUTBETALING med feilutbetalingsklassekoden.
 * Dette er den som gir `totalFeilutbetaling`.
 */
internal fun feilutbetaling(beløp: Int) =
    Testpostering(
        type = "FEILUTBETALING",
        klassekode = OppsummeringGenerator.KLASSEKODE_FEILUTBETALING,
        beløp = beløp,
    )

/**
 * FEILUTBETALING med justeringsklassekoden.
 *
 * Dette var slik justeringer kom før OS begynte å sende dem som egen posteringstype.
 * Se [OppsummeringGeneratorJusteringTest] for hvorfor skillet betyr noe.
 */
internal fun feilutbetalingMedJusteringsklassekode(beløp: Int) =
    Testpostering(
        type = "FEILUTBETALING",
        klassekode = OppsummeringGenerator.KLASSEKODE_JUSTERING,
        beløp = beløp,
    )

internal fun justering(beløp: Int) =
    Testpostering(
        type = "JUSTERING",
        klassekode = OppsummeringGenerator.KLASSEKODE_JUSTERING,
        beløp = beløp,
    )

internal fun motpostering(beløp: Int) =
    Testpostering(type = "MOTPOSTERING", klassekode = Klassekoder.MOTPOSTERING, beløp = beløp)

internal fun trekk(beløp: Int, klassekode: String = Klassekoder.TREKK_BIDRAG) =
    Testpostering(type = "TREKK", klassekode = klassekode, beløp = beløp)

internal fun forskudsskatt(beløp: Int) =
    Testpostering(
        type = "FORSKUDSSKATT",
        klassekode = Klassekoder.FORSKUDSSKATT,
        beløp = beløp,
    )

/**
 * En postering på et annet fagområde enn tiltakspenger.
 * Skal filtreres bort.
 */
internal fun annetFagområde(beløp: Int, fagområde: String = "DAGPENGER") =
    Testpostering(type = "YTELSE", klassekode = "DPORAS", beløp = beløp, fagområde = fagområde)

/**
 * Simulerer én dag med de gitte posteringene og gir tilbake den ferdig oppsummerte dagen.
 *
 * Beløpene brukes uendret, siden en periode på én dag ikke fordeles.
 */
internal fun simulerDag(vararg posteringer: Testpostering): Simuleringsdag =
    simulerDagPåDato(Simuleringstestdata.førsteDag, *posteringer)

internal fun simulerDagPåDato(dato: LocalDate, vararg posteringer: Testpostering): Simuleringsdag =
    simulerPeriode(Periode(dato, dato), *posteringer).single()

/**
 * Simulerer en sammenhengende periode med de gitte posteringene.
 *
 * Beløpene fordeles likt over dagene i perioden og avrundes per dag, slik `tilPosteringerPerDag` gjør det.
 * Bruk denne når selve fordelingen er det du vil teste; ellers er [simulerDag] enklere.
 */
internal fun simulerPeriode(periode: Periode, vararg posteringer: Testpostering): List<Simuleringsdag> =
    simulering(periode, *posteringer).simuleringPerMeldeperiode.flatMap { it.simuleringsdager }

/** Som [simulerPeriode], men gir hele simuleringen i stedet for bare dagene. */
internal fun simulering(periode: Periode, vararg posteringer: Testpostering): Simulering.Endring =
    byggRespons(periode, posteringer.toList())
        .toSimuleringFraHelvedResponse(Simuleringstestdata.meldeperiodeKjeder, fixedClock) as Simulering.Endring

/** Som [simulering], men uten å anta at resultatet er en endring. */
internal fun simuleringResultat(periode: Periode, vararg posteringer: Testpostering): Simulering =
    byggRespons(periode, posteringer.toList())
        .toSimuleringFraHelvedResponse(Simuleringstestdata.meldeperiodeKjeder, fixedClock)

/**
 * Simulerer flere enkeltdager, som kan ligge i ulike meldeperioder.
 *
 * Hver dag blir sin egen periode med `fom = tom`, slik OS gjør for dagytelser.
 * Bruk denne når det er fordelingen av dager på meldeperioder som er poenget, for eksempel når en justering skal balanseres innenfor eller på tvers av meldeperioder.
 */
internal fun simuleringForDager(vararg dager: Pair<LocalDate, List<Testpostering>>): Simulering.Endring =
    deserialize<SimuleringResponseDTO>(byggFlerdagersResponsJson(dager.toList()))
        .toSimuleringFraHelvedResponse(Simuleringstestdata.meldeperiodeKjeder, fixedClock) as Simulering.Endring

/**
 * Simulerer flere perioder, der hver periode kan spenne over flere dager.
 *
 * Dette er formen som trengs for å gjenskape motregning fra oppdragssystemet: et beløp stemplet med en periode på flere dager, som fordeles og avrundes per dag.
 * Bruk [simuleringForDager] når hver postering hører til én bestemt dag.
 */
internal fun simuleringForPerioder(vararg perioder: Pair<Periode, List<Testpostering>>): Simulering.Endring =
    deserialize<SimuleringResponseDTO>(byggFlerperiodeResponsJson(perioder.toList()))
        .toSimuleringFraHelvedResponse(Simuleringstestdata.meldeperiodeKjeder, fixedClock) as Simulering.Endring

private fun byggFlerperiodeResponsJson(perioder: List<Pair<Periode, List<Testpostering>>>): String {
    val perioderJson = perioder.sortedBy { it.first.fraOgMed }.joinToString(",") { (periode, posteringer) ->
        val posteringerJson = posteringer.joinToString(",") {
            """
            {"fagområde":"${it.fagområde}","sakId":"${Simuleringstestdata.saksnummer.verdi}",
             "fom":"${periode.fraOgMed}","tom":"${periode.tilOgMed}","beløp":${it.beløp},
             "type":"${it.type}","klassekode":"${it.klassekode}"}
            """.trimIndent()
        }
        """{"fom":"${periode.fraOgMed}","tom":"${periode.tilOgMed}","posteringer":[$posteringerJson]}"""
    }

    //language=json
    return """
    {
      "oppsummeringer": [],
      "detaljer": {
        "gjelderId": "${Simuleringstestdata.fnr.verdi}",
        "datoBeregnet": "${perioder.minOf { it.first.fraOgMed }}",
        "totalBeløp": 0,
        "perioder": [$perioderJson]
      }
    }
    """.trimIndent()
}

private fun byggFlerdagersResponsJson(dager: List<Pair<LocalDate, List<Testpostering>>>): String {
    val perioder = dager.sortedBy { it.first }.joinToString(",") { (dato, posteringer) ->
        val posteringerJson = posteringer.joinToString(",") {
            """
            {"fagområde":"${it.fagområde}","sakId":"${Simuleringstestdata.saksnummer.verdi}",
             "fom":"$dato","tom":"$dato","beløp":${it.beløp},"type":"${it.type}",
             "klassekode":"${it.klassekode}"}
            """.trimIndent()
        }
        """{"fom":"$dato","tom":"$dato","posteringer":[$posteringerJson]}"""
    }

    //language=json
    return """
    {
      "oppsummeringer": [],
      "detaljer": {
        "gjelderId": "${Simuleringstestdata.fnr.verdi}",
        "datoBeregnet": "${dager.minOf { it.first }}",
        "totalBeløp": 0,
        "perioder": [$perioder]
      }
    }
    """.trimIndent()
}

/**
 * Bygger responsen som **JSON på helveds wire-format**, og deserialiserer den.
 *
 * Vi går bevisst veien om JSON i stedet for å konstruere DTO-en direkte.
 * Da dekker testene også deserialiseringen -- feltnavn med æøå, parsing av datoer og oversettelsen av posteringstypen -- og ikke bare utregningen.
 * Formatet er verifisert mot 446 rå responser i `simulering_metadata` fra dev, som alle hadde nøyaktig disse feltene.
 */
internal fun byggResponsJson(periode: Periode, posteringer: List<Testpostering>): String {
    val totalBeløp = posteringer.filter { it.type == "YTELSE" }.sumOf { it.beløp }
    val perioder = if (posteringer.isEmpty()) {
        ""
    } else {
        val linjer = posteringer.joinToString(",\n") {
            """
            {
              "fagområde": "${it.fagområde}",
              "sakId": "${Simuleringstestdata.saksnummer.verdi}",
              "fom": "${periode.fraOgMed}",
              "tom": "${periode.tilOgMed}",
              "beløp": ${it.beløp},
              "type": "${it.type}",
              "klassekode": "${it.klassekode}"
            }
            """.trimIndent()
        }
        """
        {
          "fom": "${periode.fraOgMed}",
          "tom": "${periode.tilOgMed}",
          "posteringer": [$linjer]
        }
        """.trimIndent()
    }

    // //language=json
    return """
    {
      "oppsummeringer": [],
      "detaljer": {
        "gjelderId": "${Simuleringstestdata.fnr.verdi}",
        "datoBeregnet": "${periode.fraOgMed}",
        "totalBeløp": $totalBeløp,
        "perioder": [$perioder]
      }
    }
    """.trimIndent()
}

private fun byggRespons(periode: Periode, posteringer: List<Testpostering>): SimuleringResponseDTO =
    deserialize<SimuleringResponseDTO>(byggResponsJson(periode, posteringer))
