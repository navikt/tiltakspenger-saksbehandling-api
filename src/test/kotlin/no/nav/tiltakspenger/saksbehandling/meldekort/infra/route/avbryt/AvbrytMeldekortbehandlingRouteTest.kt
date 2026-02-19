package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgAvbrytMeldekortbehandling
import org.junit.jupiter.api.Test

class AvbrytMeldekortbehandlingRouteTest {

    @Test
    fun `saksbehandler kan avbryte meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (_, _, _, avbruttMeldekortbehandling, json) = this.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                    {
                      "begrunnelse": null,
                      "avbrutt": {
                        "avbruttTidspunkt": "2025-01-01T01:02:27.456789",
                        "begrunnelse": "begrunnelse for avbrytelse",
                        "avbruttAv": "Z12345"
                      },
                      "attesteringer": [],
                      "saksbehandler": "Z12345",
                      "opprettet": "2025-01-01T01:02:25.456789",
                      "type": "FØRSTE_BEHANDLING",
                      "meldeperiodeId": "${avbruttMeldekortbehandling.meldeperiode.id}",
                      "beregning": null,
                      "beslutter": null,
                      "simulertBeregning": null,
                      "brukersMeldekortId": "null",
                      "navkontor": "0220",
                      "periode": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      },
                      "erAvsluttet": true,
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
                      "sakId": "${avbruttMeldekortbehandling.sakId}",
                      "id": "${avbruttMeldekortbehandling.id}",
                      "godkjentTidspunkt": null,
                      "utbetalingsstatus": "AVBRUTT",
                      "tekstTilVedtaksbrev": null,
                      "status": "AVBRUTT"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan avbryte to meldekortbehandlinger på samme kjede`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sakMedFørsteAvbrutteMeldekortbehandling, _, _, avbruttMeldekortbehandling, _) = this.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(
                tac = tac,
            )!!
            val sakId = sakMedFørsteAvbrutteMeldekortbehandling.id
            val kjedeId = avbruttMeldekortbehandling.meldeperiode.kjedeId
            val (oppdatertSak) = this.opprettOgAvbrytMeldekortbehandling(
                tac = tac,
                sakId = sakId,
                kjedeId = kjedeId,
            )!!
            oppdatertSak.meldekortbehandlinger.also { meldekortbehandlinger ->
                meldekortbehandlinger.size shouldBe 2
                meldekortbehandlinger.forEach { it.status shouldBe MeldekortBehandlingStatus.AVBRUTT }
            }
        }
    }
}
