package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import org.junit.jupiter.api.Test

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
                        "fnr": "${søknad.fnr.verdi}",
                        "saksnummer": "${sak.saksnummer.verdi}",
                        "startet": "${behandling.opprettet}",
                        "sistEndret": "${behandling.sistEndret}",
                        "saksbehandler": null,
                        "beslutter": null,
                        "erUnderkjent": false,
                        "ventestatus": {
                          "erSattPåVent": false,
                          "begrunnelse": null,
                          "frist": null
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
                    "antallFiltrertPgaTilgang": 0,
                    "limit": 500,
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
            hentBenk(tac, "/benk/soknader", """{}""").let { it.antallIOversikten() shouldBe 2 }
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

            hentBenk(tac, "/benk/soknader", """{"filters": {"skjulEgneTilBeslutning": true}}""")
                .let { it.antallIOversikten() shouldBe 1 }
            hentBenk(tac, "/benk/soknader", """{}""")
                .let { it.antallIOversikten() shouldBe 2 }
        }
    }

    /**
     * Tilgangsfiltreringen skjer i servicen etter at fanen er hentet: radene telles med i totalene, men vises ikke.
     * Tilgangen avvises her etter at behandlingene er opprettet — ellers hadde opprettelsen selv blitt stoppet.
     */
    @Test
    @IsolatedDatabaseTest
    fun `rader saksbehandler ikke har tilgang til telles, men vises ikke`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakMedTilgang) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val fnrUtenTilgang = Fnr.random()
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac, fnr = fnrUtenTilgang)
            tac.tilgangsmaskinFakeClient.leggTil(
                fnrUtenTilgang,
                Tilgangsvurdering.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG,
                    begrunnelse = "test",
                    metadata = AvvistMetadata(type = "test", navIdent = "test", brukerIdent = "test"),
                ),
            )

            val respons = hentBenk(tac, "/benk/soknader", """{}""")

            objectMapper.readTree(respons)["oversikt"].let {
                it["totalAntall"].asInt() shouldBe 2
                it["antallFiltrertPgaTilgang"].asInt() shouldBe 1
                it["behandlinger"].single()["sakId"].stringValue() shouldBe sakMedTilgang.id.toString()
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

    private suspend fun ApplicationTestBuilder.hentBenk(
        tac: TestApplicationContextMedPostgres,
        path: String,
        body: String,
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.POST,
            path,
            jwt = jwt,
            forventet = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
            body = body,
        ).body
    }

    private fun String.antallIOversikten(): Int = objectMapper.readTree(this)["oversikt"]["totalAntall"].asInt()

    private fun String.fane(): String = objectMapper.readTree(this)["tab"].stringValue()

    private fun String.error(): String? = objectMapper.readTree(this)["error"].stringValue()
}
