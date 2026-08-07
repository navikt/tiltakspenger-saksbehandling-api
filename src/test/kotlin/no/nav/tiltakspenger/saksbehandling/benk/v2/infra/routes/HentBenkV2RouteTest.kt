package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import org.junit.jupiter.api.Test

/**
 * Prodstien til benk v2: én post mot `/benk` gir én fane pluss antallet i alle fanene.
 *
 * Testen pinner json-en, fordi det er den som er kontrakten mot frontendens `lib/benk/v2/typer`.
 * Kjører isolert, siden benken sveiper over hele skjemaet og ellers ville se andre testers saker.
 */
class HentBenkV2RouteTest {

    private val saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999801")

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen svarer med fanen, antall per fane og oversikten`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, søknad) = opprettSakOgSøknad(tac = tac)

            val respons = hentBenk(tac, """{"tab": "SØKNADER", "sortering": "kravtidspunkt,ASC"}""")

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
                        "sakId": "${sak.id}",
                        "fnr": "${søknad.fnr.verdi}",
                        "saksnummer": "${sak.saksnummer.verdi}",
                        "startet": "${søknad.opprettet}",
                        "sistEndret": "${søknad.opprettet}",
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
                        "resultat": null
                      }
                    ],
                    "totalAntall": 1,
                    "totalAntallUfiltrert": 1,
                    "antallFiltrertPgaTilgang": 0,
                    "limit": 500
                  }
                }
            """.trimIndent()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `filtre fra body slår gjennom til spørringen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, saksbehandler = saksbehandler)

            hentBenk(
                tac,
                """{"tab": "SØKNADER", "filters": {"status": "UNDER_BEHANDLING"}}""",
            ).let { it.antallIOversikten() shouldBe 1 }
            hentBenk(
                tac,
                """{"tab": "SØKNADER", "filters": {"saksbehandler": "IKKE_TILDELT"}}""",
            ).let { it.antallIOversikten() shouldBe 1 }
            hentBenk(tac, """{"tab": "SØKNADER"}""").let { it.antallIOversikten() shouldBe 2 }
        }
    }

    /**
     * Fanenavn, filterverdi og sorteringskolonne kommer fra en url brukeren kan redigere.
     * Da skal benken svare med noe fornuftig framfor en 400, ellers står saksbehandleren igjen med en tom side.
     */
    @Test
    @IsolatedDatabaseTest
    fun `ukjente verdier gir default framfor feil`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)

            val respons = hentBenk(
                tac,
                """{"tab": "TULL", "sortering": "drop_table,TULL", "filters": {"status": "TULL", "søknadstype": "TULL"}}""",
            )

            respons.fane() shouldBe "SØKNADER"
            respons.antallIOversikten() shouldBe 1
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `alle fanene svarer`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            listOf("SØKNADER", "REVURDERINGER", "MELDEKORT", "KLAGE", "TILBAKEKREVING").forEach { fane ->
                hentBenk(tac, """{"tab": "$fane"}""").let {
                    it.fane() shouldBe fane
                    it.antallIOversikten() shouldBe 0
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.hentBenk(
        tac: TestApplicationContextMedPostgres,
        body: String,
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.POST,
            "/benk",
            jwt = jwt,
            forventet = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
            body = body,
        ).body
    }

    private fun String.antallIOversikten(): Int = objectMapper.readTree(this)["oversikt"]["totalAntall"].asInt()

    private fun String.fane(): String = objectMapper.readTree(this)["tab"].textValue()
}
