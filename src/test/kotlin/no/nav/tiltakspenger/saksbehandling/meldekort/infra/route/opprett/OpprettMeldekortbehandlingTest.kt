package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import org.junit.jupiter.api.Test

class OpprettMeldekortbehandlingTest {
    @Test
    fun `kan opprette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(tac)
            val førsteMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
            val (_, meldekortbehandling, jsonResponse) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteMeldeperiode.kjedeId,
            )!!
            jsonResponse.toString().shouldEqualJson(
                """
                    {
                      "periodeMedÅpenBehandling": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      },
                      "avbrutteMeldekortBehandlinger": [],
                      "tiltaksnavn": [
                        "Testnavn"
                      ],
                      "sisteBeregning": null,
                      "brukersMeldekort": [],
                      "id": "2025-03-31/2025-04-13",
                      "meldeperioder": [
                        {
                          "opprettet": "${meldekortbehandling.meldeperiode.opprettet}",
                          "kjedeId": "2025-03-31/2025-04-13",
                          "girRett": {
                            "2025-03-31": false,
                            "2025-04-10": true,
                            "2025-04-11": false,
                            "2025-04-01": true,
                            "2025-04-12": false,
                            "2025-04-02": true,
                            "2025-04-13": false,
                            "2025-04-03": true,
                            "2025-04-04": true,
                            "2025-04-05": true,
                            "2025-04-06": true,
                            "2025-04-07": true,
                            "2025-04-08": true,
                            "2025-04-09": true
                          },
                          "id": "${meldekortbehandling.meldeperiode.id}",
                          "ingenDagerGirRett": false,
                          "versjon": 1,
                          "periode": {
                            "fraOgMed": "2025-03-31",
                            "tilOgMed": "2025-04-13"
                          },
                          "antallDager": 10
                        }
                      ],
                      "meldekortBehandlinger": [
                        {
                          "begrunnelse": null,
                          "avbrutt": null,
                          "attesteringer": [],
                          "saksbehandler": "Z12345",
                          "opprettet": "${meldekortbehandling.opprettet}",
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
                          "status": "UNDER_BEHANDLING"
                        }
                      ],
                      "korrigeringFraTidligerePeriode": null,
                      "periode": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      },
                      "status": "UNDER_BEHANDLING"
                    }
                """.trimIndent(),
            )
        }
    }
}
