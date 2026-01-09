package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overta

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgOverta
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutterOgOverta
import org.junit.jupiter.api.Test

class OvertaMeldekortBehandlingRouteTest {
    @Test
    fun `saksbehandler kan overta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = this.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgOverta(
                tac,
            )!!
            json.toString().shouldEqualJson(
                """
                    {
                      "begrunnelse": null,
                      "avbrutt": null,
                      "attesteringer": [],
                      "saksbehandler": "saksbehandlerSomOvertar",
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
                      "status": "UNDER_BEHANDLING"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `beslutter kan overta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutterOgOverta(
                tac = tac,
                overtarFraBeslutter = ObjectMother.beslutter("overtarFraBeslutter"),
                beslutterSomOvertar = ObjectMother.beslutter("beslutterSomOvertar"),
            )!!
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BESLUTNING
            meldekortbehandling.beslutter shouldBe "beslutterSomOvertar"
        }
    }
}
