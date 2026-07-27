package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringstestdata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ytelse
import org.junit.jupiter.api.Test

/**
 * Databasen inneholder to former av simuleringsfeltet, og begge må kunne leses.
 *
 * Den gamle formen splittet hver postering opp i én rad per dag, under `simuleringsdager[].posteringsdag`.
 * Den nye lagrer posteringene med perioden oppdragssystemet ga dem, under `perMeldeperiode[].posteringer`.
 *
 * Vi skiller dem på om `posteringer` finnes, ikke på et versjonsnummer.
 * Feltene er disjunkte, så en rad kan aldri tolkes som feil form.
 */
internal class SimuleringDbJsonTest {

    private val meldeperiodeId =
        Simuleringstestdata.meldeperiodeKjeder.hentMeldeperioderForPeriode(Simuleringstestdata.meldeperiode)
            .single().id.toString()

    /**
     * En rad skrevet før endringen leses med nøyaktig de tallene den inneholder.
     *
     * Posteringene blir endagsposteringer, fordi det er nettopp det raden inneholder.
     * Det er ingen rekonstruksjon av den opprinnelige perioden -- den informasjonen finnes ikke i raden.
     */
    @Test
    fun `gammel form leses som endagsposteringer`() {
        val simulering = gammelFormJson().toSimuleringFraDbJson(Simuleringstestdata.meldeperiodeKjeder)

        simulering.shouldVæreEndring().let {
            val meldeperiode = it.simuleringPerMeldeperiode.single()
            meldeperiode.simuleringsdager.single().nyUtbetaling shouldBe 408
            meldeperiode.posteringer.single().periode shouldBe
                Periode(6.januar(2025), 6.januar(2025))
            meldeperiode.posteringer.single().beløp shouldBe 408
            meldeperiode.posteringer.single().type shouldBe Posteringstype.YTELSE
        }
    }

    /**
     * De lagrede aggregatene på en gammel rad regnes ikke om.
     *
     * Raden under har `totalTrekk = 250`, som er tallet den gamle utregningen ga: bare de positive trekkposteringene.
     * Med dagens regel ville de samme posteringene gitt -224.
     * Vi leser likevel 250, slik at en lukket behandling viser det saksbehandler faktisk så da vedtaket ble fattet.
     */
    @Test
    fun `lagrede aggregater på gammel rad regnes ikke om`() {
        val simulering = gammelFormMedTrekkJson().toSimuleringFraDbJson(Simuleringstestdata.meldeperiodeKjeder)

        val dag = simulering.shouldVæreEndring().simuleringPerMeldeperiode.single().simuleringsdager.single()
        dag.totalTrekk shouldBe 250
    }

    /** Det vi skriver i dag, leses tilbake til nøyaktig det samme. */
    @Test
    fun `ny form skrives og leses tilbake uendret`() {
        val original = simulering(
            Periode(6.januar(2025), 10.januar(2025)),
            ytelse(1490),
            trekk(-191),
        )

        val lest = original.toDbJson().toSimuleringFraDbJson(Simuleringstestdata.meldeperiodeKjeder)

        lest shouldBe original
    }

    /** Posteringens egen periode overlever lagringen, ikke bare dagsverdiene som utledes av den. */
    @Test
    fun `perioden på posteringen bevares gjennom lagring`() {
        val original = simulering(Periode(6.januar(2025), 10.januar(2025)), ytelse(1490), trekk(-191))

        val lest = original.toDbJson().toSimuleringFraDbJson(Simuleringstestdata.meldeperiodeKjeder)

        val trekkpostering = lest.shouldVæreEndring().simuleringPerMeldeperiode.single()
            .posteringer.single { it.type == Posteringstype.TREKK }
        trekkpostering.periode shouldBe Periode(6.januar(2025), 10.januar(2025))
        trekkpostering.beløp shouldBe -191
    }

    /** Den nye formen skriver ikke lenger posteringer per dag. */
    @Test
    fun `ny form skriver posteringene på meldeperioden`() {
        val json = simulering(Periode(6.januar(2025), 10.januar(2025)), ytelse(1490)).toDbJson()

        val dbJson = deserialize<SimuleringDbJson>(json)
        val meldeperiode = dbJson.simulering!!.perMeldeperiode.single()
        meldeperiode.posteringer!!.single().beløp shouldBe 1490
        meldeperiode.simuleringsdager.all { it.posteringsdag == null } shouldBe true
    }

    private fun Simulering.shouldVæreEndring(): Simulering.Endring {
        (this is Simulering.Endring) shouldBe true
        return this as Simulering.Endring
    }

    //language=json
    private fun gammelFormJson(): String = """
    {
      "type": "ENDRING",
      "simuleringstidspunkt": "2025-01-06T12:00:00",
      "simulering": {
        "datoBeregnet": "2025-01-06",
        "totalBeløp": 408,
        "perMeldeperiode": [
          {
            "meldeperiodeId": "$meldeperiodeId",
            "simuleringsdager": [
              {
                "dato": "2025-01-06",
                "tidligereUtbetalt": 0,
                "nyUtbetaling": 408,
                "totalEtterbetaling": 408,
                "totalFeilutbetaling": 0,
                "totalMotpostering": 0,
                "totalTrekk": 0,
                "totalJustering": 0,
                "harJustering": false,
                "posteringsdag": {
                  "dato": "2025-01-06",
                  "posteringer": [
                    {
                      "dato": "2025-01-06",
                      "fagområde": "TILTAKSPENGER",
                      "beløp": 408,
                      "type": "YTELSE",
                      "klassekode": "TPTPAFT"
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
    }
    """.trimIndent()

    //language=json
    private fun gammelFormMedTrekkJson(): String = """
    {
      "type": "ENDRING",
      "simuleringstidspunkt": "2025-01-06T12:00:00",
      "simulering": {
        "datoBeregnet": "2025-01-06",
        "totalBeløp": 0,
        "perMeldeperiode": [
          {
            "meldeperiodeId": "$meldeperiodeId",
            "simuleringsdager": [
              {
                "dato": "2025-01-06",
                "tidligereUtbetalt": 0,
                "nyUtbetaling": 0,
                "totalEtterbetaling": 224,
                "totalFeilutbetaling": 0,
                "totalMotpostering": 0,
                "totalTrekk": 250,
                "totalJustering": 0,
                "harJustering": false,
                "posteringsdag": {
                  "dato": "2025-01-06",
                  "posteringer": [
                    {
                      "dato": "2025-01-06",
                      "fagområde": "TILTAKSPENGER",
                      "beløp": -474,
                      "type": "TREKK",
                      "klassekode": "KREDKRED"
                    },
                    {
                      "dato": "2025-01-06",
                      "fagområde": "TILTAKSPENGER",
                      "beløp": 250,
                      "type": "TREKK",
                      "klassekode": "KREDKRED"
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
    }
    """.trimIndent()
}
