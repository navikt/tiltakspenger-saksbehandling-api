package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbake

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgLeggTilbake
import org.junit.jupiter.api.Test

class LeggTilbakeMeldekortbehandlingRouteTest {

    @Test
    fun `saksbehandler kan legge tilbake meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(tac)!!
            json.toString().shouldEqualJson(
                """
                    {
                      "begrunnelse": null,
                      "avbrutt": null,
                      "attesteringer": [],
                      "saksbehandler": null,
                      "opprettet": "2025-05-01T01:02:23.456789",
                      "type": "FØRSTE_BEHANDLING",
                      "meldeperiodeId": "${meldekortbehandling.meldeperiode.id}",
                      "beregning": null,
                      "beslutter": null,
                      "simulertBeregning": null,
                      "brukersMeldekortId": "null",
                      "navkontor": "0220",
                      "periode": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      },
                      "erAvsluttet": false,
                      "navkontorNavn": "Nav Asker",
                      "dager": [
                        {
                          "dato": "2025-03-31",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "dato": "2025-04-01",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-02",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-03",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-04",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-05",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-06",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-07",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-08",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-09",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-10",
                          "status": "IKKE_BESVART"
                        },
                        {
                          "dato": "2025-04-11",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "dato": "2025-04-12",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "dato": "2025-04-13",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        }
                      ],
                      "sakId": "${meldekortbehandling.sakId}",
                      "id": "${meldekortbehandling.id}",
                      "godkjentTidspunkt": null,
                      "utbetalingsstatus": "IKKE_GODKJENT",
                      "tekstTilVedtaksbrev": null,
                      "status": "KLAR_TIL_BEHANDLING"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `beslutter kan legge tilbake meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgLeggTilbake(
                tac = tac,
            )!!
        }
    }
}
