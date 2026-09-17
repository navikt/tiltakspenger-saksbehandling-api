package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import arrow.core.left
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

/**
 * Prodstien til benk v2: én post per fane under `/benk` gir fanen pluss antallet i alle fanene.
 *
 * Testen pinner json-en, fordi det er den som er kontrakten mot frontendens `lib/benk/v2/typer`.
 * Kjører isolert, siden benken sveiper over hele skjemaet og ellers ville se andre testers saker.
 */
class HentBenkRouteTest {

    private val saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999801")

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen svarer med fanen, antall per fane og oversikten`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, søknad, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val respons = hentBenk(tac, "/benk/soknader", """{"sortering": "kravtidspunkt,ASC"}""")

            respons shouldEqualJson """
                {
                  "harTilgang": true,
                  "tab": "SØKNADER",
                  "antallPerTab": {
                    "SØKNADER": 1,
                    "REVURDERINGER": 0,
                    "MELDEKORT": 0,
                    "KLAGE": 0,
                    "TILBAKEKREVING": 0
                  },
                  "oversikt": {
                    "behandlinger": [
                      {
                        "type": "SØKNADSBEHANDLING",
                        "id": "${behandling.id}",
                        "sakId": "${sak.id}",
                        "fnr": {"verdi": "${søknad.fnr.verdi}", "erSladdet": false},
                        "saksnummer": "${sak.saksnummer.verdi}",
                        "startet": "${behandling.opprettet}",
                        "sistEndret": "${behandling.sistEndret}",
                        "saksbehandler": null,
                        "beslutter": null,
                        "erUnderkjent": false,
                        "ventestatus": {
                          "erSattPåVent": false,
                          "begrunnelse": {"verdi": null, "erSladdet": false},
                          "frist": null
                        },
                        "tilgang": {
                          "vurdering": "HAR_TILGANG",
                          "grunn": null
                        },
                        "personmarkører": {
                          "skjermet": false,
                          "kode6": false,
                          "kode7": false
                        },
                        "status": "KLAR_TIL_BEHANDLING",
                        "søknadstype": "DIGITAL",
                        "kravtidspunkt": "${søknad.opprettet}",
                        "resultat": "IKKE_VALGT",
                        "gyldigeKommandoer": ["TildelSaksbehandler", "Avbryt"]
                      }
                    ],
                    "totalAntall": 1,
                    "totalAntallUfiltrert": 1,
                    "oppsummering": {
                      "antallMedTilgang": 1,
                      "antallUtenTilgang": 0,
                      "antallSkjermet": 0,
                      "antallKode6": 0,
                      "antallKode7": 0
                    },
                    "side": 0,
                    "sideantall": 200,
                    "saksbehandlere": [],
                    "besluttere": []
                  },
                  "error": null
                }
            """.trimIndent()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `filtre fra body slår gjennom til spørringen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, saksbehandler = saksbehandler)

            hentBenk(
                tac,
                "/benk/soknader",
                """{"filters": {"status": "UNDER_BEHANDLING"}}""",
            ).let { it.antallIOversikten() shouldBe 1 }
            hentBenk(
                tac,
                "/benk/soknader",
                """{"filters": {"saksbehandler": "IKKE_TILDELT"}}""",
            ).let { it.antallIOversikten() shouldBe 2 }
            hentBenk(
                tac,
                "/benk/soknader",
                """{"filters": {"saksbehandler": "IKKE_TILDELT_SAKSBEHANDLER"}}""",
            ).let { it.antallIOversikten() shouldBe 1 }
            hentBenk(tac, "/benk/soknader", """{}""").antallIOversikten() shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `raden viser kommandoene den innloggede saksbehandleren kan utføre`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, saksbehandler = saksbehandler)

            val respons = hentBenk(tac, "/benk/soknader", """{}""")

            objectMapper.readTree(respons)["oversikt"]["behandlinger"].single()["gyldigeKommandoer"]
                .toString() shouldEqualJson """["LeggTilbakeSaksbehandler", "SettPåVent", "Avbryt"]"""
        }
    }

    /**
     * Fanenavnet og filterverdiene kommer fra en url brukeren kan redigere.
     * Da skal benken svare med en standardvisning og et error-felt frontenden kan vise, framfor en 400 og en tom side.
     */
    @Test
    @IsolatedDatabaseTest
    fun `feilskrevet fane i url-en gir søknadsfanen med error`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val respons = hentBenk(tac, "/benk/tull", """{}""")

            respons.fane() shouldBe "SØKNADER"
            respons.antallIOversikten() shouldBe 1
            respons.error() shouldBe "Fanen finnes ikke, så søknadsfanen vises"
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `ugyldige filterverdier i body gir standardvisningen med error`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val respons = hentBenk(
                tac,
                "/benk/soknader",
                """{"filters": {"status": "TULL", "søknadstype": "TULL"}}""",
            )

            respons.fane() shouldBe "SØKNADER"
            respons.antallIOversikten() shouldBe 1
            respons.error() shouldBe "Noen av filterverdiene kunne ikke tolkes, så standardvisningen brukes"
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `ukjent sorteringskolonne gir default sortering uten error`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val respons = hentBenk(tac, "/benk/soknader", """{"sortering": "drop_table,TULL"}""")

            respons.antallIOversikten() shouldBe 1
            respons.error() shouldBe null
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `bar benk-url svarer med søknadsfanen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val respons = hentBenk(tac, "/benk", """{"tab": "SØKNADER"}""")

            respons.fane() shouldBe "SØKNADER"
            respons.antallIOversikten() shouldBe 1
            respons.error() shouldBe null
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `skjulEgneTilBeslutning tar bort behandlingene innlogget saksbehandler har sendt til beslutning`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            sendSøknadsbehandlingTilBeslutning(tac = tac, saksbehandler = saksbehandler)
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            hentBenk(tac, "/benk/soknader", """{"filters": {"skjulEgneTilBeslutning": true}}""").antallIOversikten() shouldBe 1
            hentBenk(tac, "/benk/soknader", """{}""").antallIOversikten() shouldBe 2
        }
    }

    /**
     * Markørene kommer fra regelen Tilgangsmaskinen avviste på, så de er bare satt for rader uten tilgang.
     * Benken gjør ingen oppslag mot PDL eller skjermingsregisteret.
     */
    @Test
    @IsolatedDatabaseTest
    fun `alle rader vises med tilgang, personmarkører og oppsummering`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakMedTilgang) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val avvisteSaker = listOf(
                TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG,
                TilgangsvurderingAvvistÅrsak.FORTROLIG,
                TilgangsvurderingAvvistÅrsak.SKJERMET,
                TilgangsvurderingAvvistÅrsak.UKJENT,
            ).associateWith { årsak ->
                val fnr = Fnr.random()
                val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac, fnr = fnr)
                tac.tilgangsmaskinFakeClient.leggTil(
                    fnr,
                    Tilgangsvurdering.Avvist(
                        årsak = årsak,
                        begrunnelse = "Du har ikke tilgang",
                        metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = "test", brukerIdent = fnr),
                    ),
                )
                sak
            }

            val respons = hentBenk(tac, "/benk/soknader", """{}""")

            objectMapper.readTree(respons)["oversikt"].let { oversikt ->
                oversikt["totalAntall"].asInt() shouldBe 5
                oversikt.has("antallFiltrertPgaTilgang") shouldBe false
                oversikt["behandlinger"].size() shouldBe 5
                oversikt["oppsummering"].toString() shouldEqualJson """
                    {
                      "antallMedTilgang": 1,
                      "antallUtenTilgang": 4,
                      "antallSkjermet": 1,
                      "antallKode6": 1,
                      "antallKode7": 1
                    }
                """.trimIndent()

                val radMedTilgang = oversikt.rad(sakMedTilgang.id.toString())
                radMedTilgang["tilgang"].toString() shouldEqualJson """
                    {"vurdering": "HAR_TILGANG", "grunn": null}
                """.trimIndent()
                radMedTilgang["personmarkører"].toString() shouldEqualJson """
                    {"skjermet": false, "kode6": false, "kode7": false}
                """.trimIndent()
                radMedTilgang["fnr"]["erSladdet"].asBoolean() shouldBe false

                // Wirenavnet er benkens egen kontrakt, så det står i klartekst her i stedet for å bli utledet av domeneenumen.
                val forventetÅrsaksnavn = mapOf(
                    TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG to "STRENGT_FORTROLIG_ADRESSE",
                    TilgangsvurderingAvvistÅrsak.FORTROLIG to "FORTROLIG_ADRESSE",
                    TilgangsvurderingAvvistÅrsak.SKJERMET to "SKJERMET",
                    TilgangsvurderingAvvistÅrsak.UKJENT to "UKJENT",
                )

                mapOf(
                    TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG to """{"skjermet": false, "kode6": true, "kode7": false}""",
                    TilgangsvurderingAvvistÅrsak.FORTROLIG to """{"skjermet": false, "kode6": false, "kode7": true}""",
                    TilgangsvurderingAvvistÅrsak.SKJERMET to """{"skjermet": true, "kode6": false, "kode7": false}""",
                    // En kode vi ikke kjenner, gir ingen markør; raden sladdes fordi tilgangen er avvist.
                    TilgangsvurderingAvvistÅrsak.UKJENT to """{"skjermet": false, "kode6": false, "kode7": false}""",
                ).forEach { (årsak, forventedeMarkører) ->
                    val rad = oversikt.rad(avvisteSaker.getValue(årsak).id.toString())
                    rad["tilgang"].toString() shouldEqualJson """
                        {
                          "vurdering": "HAR_IKKE_TILGANG",
                          "grunn": {"årsak": "${forventetÅrsaksnavn.getValue(årsak)}", "begrunnelse": "Du har ikke tilgang"}
                        }
                    """.trimIndent()
                    rad["personmarkører"].toString() shouldEqualJson forventedeMarkører
                    rad["fnr"].toString() shouldEqualJson """{"verdi": null, "erSladdet": true}"""
                    rad["ventestatus"]["begrunnelse"].toString() shouldEqualJson """{"verdi": null, "erSladdet": true}"""
                    rad["gyldigeKommandoer"].size() shouldBe 0
                }
            }
        }
    }

    /**
     * Tilgangen er først kjent etter oppslaget mot Tilgangsmaskinen, så filteret skjer etter pagineringen.
     * `totalAntall` teller derfor fortsatt radene før tilgangsfiltreringen, mens listen og oppsummeringen bare har radene med tilgang.
     */
    @Test
    @IsolatedDatabaseTest
    fun `skjulUtenTilgang tar bort radene innlogget saksbehandler ikke har tilgang til`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val fnrUtenTilgang = Fnr.random()
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac, fnr = fnrUtenTilgang)
            tac.tilgangsmaskinFakeClient.leggTil(
                fnrUtenTilgang,
                Tilgangsvurdering.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.SKJERMET,
                    begrunnelse = "Du har ikke tilgang",
                    metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = "test", brukerIdent = fnrUtenTilgang),
                ),
            )

            hentBenk(tac, "/benk/soknader", """{}""").antallIOversikten() shouldBe 2
            hentBenk(tac, "/benk/soknader", """{"filters": {"skjulUtenTilgang": true}}""").let { respons ->
                objectMapper.readTree(respons)["oversikt"].let { oversikt ->
                    oversikt["totalAntall"].asInt() shouldBe 1
                    oversikt["behandlinger"].size() shouldBe 1
                    oversikt["oppsummering"].toString() shouldEqualJson """
                        {
                          "antallMedTilgang": 1,
                          "antallUtenTilgang": 1,
                          "antallSkjermet": 1,
                          "antallKode6": 0,
                          "antallKode7": 0
                        }
                    """.trimIndent()
                }
            }
        }
    }

    /**
     * Veileder og utvikler har ikke rolle for å se benken.
     * Ruten svarer dem med det tomme svaret så tidlig som mulig, uten databaseoppslag eller tilgangskall.
     */
    @Test
    @IsolatedDatabaseTest
    fun `brukere uten benkrolle får tomt svar`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            listOf(ObjectMother.veileder(), ObjectMother.veileder()).forEach { bruker ->
                hentBenk(tac, "/benk/soknader", """{}""", saksbehandler = bruker).let {
                    it shouldEqualJson """{"harTilgang": false}"""
                }
            }
        }
    }

    /**
     * Saksbehandlerdekningen står i den pinned json-testen over, og `kanSeBenken` er rolletestet i SladdingTest.
     * Tilbakekreving testes ikke her, fordi rollen ikke er i `alleAdRoller` og derfor stoppes i autentiseringen med 403.
     */
    @Test
    @IsolatedDatabaseTest
    fun `beslutter kan se benken`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            hentBenk(tac, "/benk/soknader", """{}""", saksbehandler = ObjectMother.beslutter()).let {
                objectMapper.readTree(it)["harTilgang"].asBoolean() shouldBe true
                it.antallIOversikten() shouldBe 1
            }
        }
    }

    /**
     * Sidetallet gjenspeiles i responsen, og en side forbi slutten svarer med tom side — ikke en feil.
     * Et negativt sidetall fra en url brukeren kan redigere faller tilbake på side 0, slik de øvrige ugyldige verdiene gjør.
     */
    @Test
    @IsolatedDatabaseTest
    fun `sidetallet gjenspeiles i responsen, og ugyldig side faller tilbake på første side`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            hentBenk(tac, "/benk/soknader", """{"side": 1}""").let {
                objectMapper.readTree(it)["oversikt"].let { oversikt ->
                    oversikt["side"].asInt() shouldBe 1
                    oversikt["totalAntall"].asInt() shouldBe 1
                    oversikt["behandlinger"].size() shouldBe 0
                }
            }
            hentBenk(tac, "/benk/soknader", """{"side": -1}""").let {
                objectMapper.readTree(it)["oversikt"].let { oversikt ->
                    oversikt["side"].asInt() shouldBe 0
                    oversikt["behandlinger"].size() shouldBe 1
                }
                it.error() shouldBe null
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `alle fanene svarer`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            mapOf(
                "soknader" to "SØKNADER",
                "revurderinger" to "REVURDERINGER",
                "meldekort" to "MELDEKORT",
                "klage" to "KLAGE",
                "tilbakekreving" to "TILBAKEKREVING",
            ).forEach { (path, fane) ->
                hentBenk(tac, "/benk/$path", """{}""").let {
                    it.fane() shouldBe fane
                    it.antallIOversikten() shouldBe 0
                    it.error() shouldBe null
                }
            }
        }
    }

    /**
     * Uten en tilgangsvurdering vet vi ikke hvilke rader saksbehandleren har lov til å se.
     * Da svarer benken med en serverfeil framfor å vise radene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `benken svarer 500 når tilgangskontrollen feiler`() {
        withTestApplicationContextAndPostgres(
            tilgangsmaskinFakeClient = object : TilgangsmaskinFakeTestClient() {
                override suspend fun harTilgangTilPersoner(
                    fnrs: List<Fnr>,
                    saksbehandlerToken: String,
                ) = TilgangskontrollFeil.ForMangeIdenter.left()
            },
            runIsolated = true,
        ) { tac ->
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            hentBenk(
                tac,
                "/benk/soknader",
                """{}""",
                forventet = ForventetRespons.json(
                    500,
                    """
                    {
                      "melding": "Noe gikk galt på serversiden",
                      "kode": "server_feil"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.hentBenk(
        tac: TestApplicationContextMedPostgres,
        path: String,
        body: String,
        saksbehandler: Saksbehandler = this@HentBenkRouteTest.saksbehandler,
        forventet: ForventetRespons = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.POST,
            path,
            jwt = jwt,
            forventet = forventet,
            body = body,
        ).body
    }

    /** Radene kommer i spørringens rekkefølge, så testene slår dem opp på sakId framfor posisjon. */
    private fun JsonNode.rad(sakId: String): JsonNode =
        this["behandlinger"].first { it["sakId"].stringValue() == sakId }

    private fun String.antallIOversikten(): Int = objectMapper.readTree(this)["oversikt"]["totalAntall"].asInt()

    private fun String.fane(): String = objectMapper.readTree(this)["tab"].stringValue()

    private fun String.error(): String? = objectMapper.readTree(this)["error"].stringValue()
}
