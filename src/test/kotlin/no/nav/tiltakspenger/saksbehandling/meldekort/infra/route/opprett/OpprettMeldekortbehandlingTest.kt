package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
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
            jsonResponse.toString().shouldEqualJsonIgnoringTimestamps(
                """
                    {
                      "periodeMedÅpenBehandling": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      },
                      "avbrutteMeldekortbehandlinger": [],
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
                          "id": "${meldekortbehandling.meldeperiodeLegacy.id}",
                          "ingenDagerGirRett": false,
                          "versjon": 1,
                          "periode": {
                            "fraOgMed": "2025-03-31",
                            "tilOgMed": "2025-04-13"
                          },
                          "antallDager": 10
                        }
                      ],
                      "meldekortbehandlinger": [
                        {
                          "begrunnelse": null,
                          "avbrutt": null,
                          "attesteringer": [],
                          "saksbehandler": "Z12345",
                          "opprettet": "2025-05-01T01:02:26.456789",
                          "kanIkkeIverksetteUtbetaling": null,
                          "tilbakekrevingId": null,
                          "type": "FØRSTE_BEHANDLING",
                          "meldeperiodeId": "${meldekortbehandling.meldeperiodeLegacy.id}",
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
                          "status": "UNDER_BEHANDLING",
                          "skalSendeVedtaksbrev": true,
                          "harFlereMeldeperioder": false,
                          "ventestatus": []
                        }
                      ],
                      "meldekortbehandlingerV2": [
                        {
                          "begrunnelse": null,
                          "avbrutt": null,
                          "attesteringer": [],
                          "saksbehandler": "Z12345",
                          "opprettet": "TIMESTAMP",
                          "kanIkkeIverksetteUtbetaling": null,
                          "tilbakekrevingId": null,
                          "meldeperioder": [
                            {
                              "kjedeId": "2025-03-31/2025-04-13",
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
                              "meldeperiodeId": "${meldekortbehandling.meldeperiodeLegacy.id}",
                              "brukersMeldekortId": null,
                              "periode": {
                                "fraOgMed": "2025-03-31",
                                "tilOgMed": "2025-04-13"
                              }
                            }
                          ],
                          "type": "FØRSTE_BEHANDLING",
                          "beregning": null,
                          "beslutter": null,
                          "simulertBeregning": null,
                          "navkontor": "0220",
                          "periode": {
                            "fraOgMed": "2025-03-31",
                            "tilOgMed": "2025-04-13"
                          },
                          "erAvsluttet": false,
                          "navkontorNavn": "Nav Asker",
                          "sakId": "${meldekortbehandling.sakId}",
                          "id": "${meldekortbehandling.id}",
                          "godkjentTidspunkt": null,
                          "utbetalingsstatus": "IKKE_GODKJENT",
                          "tekstTilVedtaksbrev": null,
                          "status": "UNDER_BEHANDLING",
                          "skalSendeVedtaksbrev": true,
                           "ventestatus": []
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
            val (oppdatertSak) = this.iverksettOmgjøringInnvilgelse(
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
                medJsonBody = {
                    it harKode "INGEN_DAGER_GIR_RETT"
                },
            )
        }
    }

    @Test
    fun `kan ikke opprette meldekortbehandling dersom det allerede finnes en åpen behandling på kjeden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 10.april(2025)),
            )
            val kjedeId = sak.meldeperiodeKjeder.first().kjedeId

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "HAR_ÅPEN_BEHANDLING" },
            )

            val sakEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakEtter.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan ikke opprette meldekortbehandling på senere kjede før første kjede er behandlet`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1 til 31.januar(2025)),
            )
            sak.meldeperiodeKjeder.size shouldBe 3
            val andreKjedeId = sak.meldeperiodeKjeder[1].kjedeId

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "MÅ_BEHANDLE_FØRSTE_KJEDE" },
            )

            val sakEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakEtter.meldekortbehandlinger.shouldBeEmpty()
        }
    }

    @Test
    fun `kan opprette behandling for meldeperiode uten rett dersom det finnes et ubehandlet brukers-meldekort`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )
            val førsteKjede = sak.meldeperiodeKjeder.first()
            val førsteMeldeperiode = førsteKjede.hentSisteMeldeperiode()

            // Send brukers meldekort på første kjede mens den fortsatt gir rett
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = førsteMeldeperiode.id,
                sakId = sak.id,
                dager = førsteMeldeperiode.tilUtfyltFraBruker(),
            )

            // Omgjør slik at kun andre kjede beholder rett (første kjede mister all rett)
            val andreKjedePeriode = sak.meldeperiodeKjeder[1].periode
            this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(andreKjedePeriode),
            )

            val (oppdatertSak, meldekortbehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
            )!!

            meldekortbehandling.kjedeIdLegacy shouldBe førsteKjede.kjedeId
            meldekortbehandling.meldeperiodeLegacy.ingenDagerGirRett shouldBe true
            oppdatertSak.meldekortbehandlinger.single() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `kan ikke opprette behandling på kjede uten rett dersom forrige kjede har ubehandlet brukers-meldekort`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3
            val førsteKjede = sak.meldeperiodeKjeder[0]
            val andreKjede = sak.meldeperiodeKjeder[1]
            val førsteMeldeperiode = førsteKjede.hentSisteMeldeperiode()
            val andreMeldeperiode = andreKjede.hentSisteMeldeperiode()

            // Send brukers meldekort på de to første kjedene mens de fortsatt gir rett
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = førsteMeldeperiode.id,
                sakId = sak.id,
                dager = førsteMeldeperiode.tilUtfyltFraBruker(),
            )
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = andreMeldeperiode.id,
                sakId = sak.id,
                dager = andreMeldeperiode.tilUtfyltFraBruker(),
            )

            // Omgjør slik at kun tredje kjede beholder rett (første og andre kjede mister rett)
            this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(27 til 31.januar(2025)),
            )

            // Andre kjede kan ikke behandles før første kjede er behandlet, selv om begge har et ubehandlet meldekort
            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "INGEN_DAGER_GIR_RETT" },
            )

            // Første kjede kan derimot behandles
            val (_, førsteBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
            )!!
            førsteBehandling.kjedeIdLegacy shouldBe førsteKjede.kjedeId
            førsteBehandling.meldeperiodeLegacy.ingenDagerGirRett shouldBe true
        }
    }

    @Test
    fun `kan avbryte to påfølgende kjeder uten rett`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3
            val førsteKjede = sak.meldeperiodeKjeder[0]
            val andreKjede = sak.meldeperiodeKjeder[1]
            val førsteMeldeperiode = førsteKjede.hentSisteMeldeperiode()
            val andreMeldeperiode = andreKjede.hentSisteMeldeperiode()

            // 1. Brukers meldekort på begge kjeder mens de fortsatt gir rett, og omgjøring fjerner all rett fra de to første kjedene
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = førsteMeldeperiode.id,
                sakId = sak.id,
                dager = førsteMeldeperiode.tilUtfyltFraBruker(),
            )
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = andreMeldeperiode.id,
                sakId = sak.id,
                dager = andreMeldeperiode.tilUtfyltFraBruker(),
            )
            this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(27 til 31.januar(2025)),
            )

            // 2. Andre kjede kan ikke behandles enda - første kjede har ubehandlet meldekort foran
            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "INGEN_DAGER_GIR_RETT" },
            )

            // 3. Opprett behandling på første kjede og avbryt den
            val (_, førsteBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
            )!!
            val (sakEtterAvbryt, avbruttBehandling, _) = avbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
            )!!
            avbruttBehandling.erAvbrutt shouldBe true
            sakEtterAvbryt.meldekortbehandlinger.single() shouldBe avbruttBehandling

            // 4. Nå skal det være mulig å opprette behandling på andre kjede
            val (_, andreBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
            )!!
            andreBehandling.kjedeIdLegacy shouldBe andreKjede.kjedeId
            andreBehandling.meldeperiodeLegacy.ingenDagerGirRett shouldBe true
        }
    }
}
