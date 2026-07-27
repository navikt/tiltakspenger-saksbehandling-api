package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.utbetaling.simulertBeregningForEnesteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.verifiserSimuleringsoppsummering
import no.nav.tiltakspenger.saksbehandling.utbetaling.verifiserSimulerteBeløp
import no.nav.tiltakspenger.saksbehandling.utbetaling.verifiserSimulerteBeløpForDag
import org.junit.jupiter.api.Test

/**
 * Ende-til-ende mot ekte database, fra meldekortbehandling til JSON-en klienten får.
 *
 * Simuleringen emuleres av `genererSimuleringFraBeregning`, som bygger responsen slik helved ville sendt den og kjører den gjennom den ekte mapperen.
 * Testen dekker dermed hele kjeden: responsformatet, deserialiseringen, `OppsummeringGenerator`, lagring og lesing fra databasen, og DTO-laget.
 *
 * Vi asserter på JSON-en, ikke på domeneobjektene, slik at et brudd i kontrakten mot frontend også fanges.
 */
internal class SimuleringRouteTest {

    /**
     * Førstegangsutbetaling: ingenting er utbetalt fra før, så alt er ny utbetaling og etterbetaling.
     *
     * Dette er den klart vanligste formen -- 4946 av dagene i dev-uttrekket besto bare av positive ytelser.
     */
    @Test
    fun `simuleringen for en førstegangsutbetaling kommer ut på routen`() {
        runTest {
            withTestApplicationContext { tac ->
                val (_, _, _, _, sakJson) = this.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
                    tac = tac,
                    saksbehandler = saksbehandler("saksbehandler"),
                )!!

                val simulertBeregning = sakJson.simulertBeregningForEnesteMeldekortbehandling()

                simulertBeregning.verifiserSimuleringsoppsummering(
                    forventetResultat = "ENDRING",
                    forventetTotalBeløp = 2682,
                    // datoBeregnet settes av klokka, ikke av meldeperioden.
                    forventetSimuleringsdato = 1.mai(2025),
                )

                //language=json
                simulertBeregning.verifiserSimulerteBeløp(
                    """
                    {
                      "etterbetaling": 2682,
                      "nyUtbetaling": 2682,
                      "tidligereUtbetaling": 0,
                      "feilutbetaling": 0,
                      "totalJustering": 0,
                      "totalTrekk": 0
                    }
                    """.trimIndent(),
                )

                // Posteringene ligger ikke lenger per dag i API-et.
                // De henger på meldeperioden med perioden oppdragssystemet ga dem, og dekkes av SimuleringDbJsonTest.
                //language=json
                simulertBeregning.verifiserSimulerteBeløpForDag(
                    dato = 1.april(2025),
                    forventet = """
                    {
                      "etterbetaling": 298,
                      "nyUtbetaling": 298,
                      "tidligereUtbetaling": 0,
                      "feilutbetaling": 0,
                      "totalJustering": 0,
                      "totalTrekk": 0
                    }
                    """.trimIndent(),
                )
            }
        }
    }
}
