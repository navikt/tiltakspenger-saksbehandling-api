package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import org.junit.jupiter.api.Test

class OpprettMeldekortbehandlingTest {
    @Test
    fun `kan opprette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 10.april(2025)),
            )
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
                        "Arbeidsmarkedsoppfølging gruppe"
                      ],
                      "sisteBeregning": null,
                      "brukersMeldekort": [],
                      "id": "2025-03-31/2025-04-13",
                      "meldeperioder": [
                        {
                          "opprettet": "2025-05-01T01:02:22.456789",
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

    @Test
    fun `kan ikke opprette meldekortbehandling for meldeperiode som ikke gir rett`() {
        // 1. Iverksetter innvilget søknadsbehandling for jan 2025
        // 2. Iverksetter omgjøring som opphører alt bortsett fra 1. jan 2025
        withTestApplicationContext { tac ->
            val innvilgelsesperiodeSøknadsbehandling: Periode = 1 til 31.januar(2025)
            val innvilgelsesperiodeOmgjøring: Periode = 1 til 1.januar(2025)

            val (sak, _, rammevedtakSøknadsbehandling, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiodeSøknadsbehandling),
            )
            val (oppdatertSak) = this.iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiodeOmgjøring),
            )
            oppdatertSak.meldeperiodeKjeder.size shouldBe 3
            oppdatertSak.meldeperiodeKjeder[0].last().antallDagerSomGirRett shouldBe 1
            oppdatertSak.meldeperiodeKjeder[1].last().antallDagerSomGirRett shouldBe 0
            oppdatertSak.meldeperiodeKjeder[2].last().antallDagerSomGirRett shouldBe 0
            val andreMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[1]
            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreMeldeperiode.kjedeId,
                forventetStatus = BadRequest,
                forventetJsonBody = """{"melding":"Meldeperiodekjeden er i en tilstand som ikke tillater å opprette en behandling: INGEN_DAGER_GIR_RETT","kode":"INGEN_DAGER_GIR_RETT"}""",
            )
        }
    }
}
