package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dev.registrerUtviklingsruter
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.saksoppslag.utviklingstilgangForSaksoppslag
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory

class MaskinSakRouteTest {
    @Test
    fun `kall uten token gir 401`() = medMaskinrute {
        client.get("/sak/ugyldig").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `maskin uten klient-ID eller med blank klient-ID får 403 før validering`() = medMaskinrute {
        listOf("maskin-uten-klient", "maskin-med-blank-klient").forEach { token ->
            val respons = client.get("/sak/ugyldig?rolle=") { bearerAuth(token) }
            respons.status shouldBe HttpStatusCode.Forbidden
            JSONObject(respons.bodyAsText()).getString("kode") shouldBe "ingen_tilgang"
        }
    }

    @Test
    fun `ugyldig saksnummer gir 400`() = medMaskinrute {
        client.get("/sak/ugyldig") { bearerAuth("maskin") }.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `ukjent saksnummer gir 404`() = medMaskinrute { kontekst ->
        val saksnummer = Saksnummer.genererSaknummer(clock = kontekst.clock, løpenr = "9999")
        val respons = client.get("/sak/${saksnummer.verdi}") { bearerAuth("maskin") }
        respons.status shouldBe HttpStatusCode.NotFound
        respons.bodyAsText() shouldBe "Fant ikke sak med saksnummer $saksnummer"
    }

    @Test
    fun `maskin får usladdet sak med kommandoer for valgt testaktør`() = medMaskinrute { kontekst ->
        val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac = kontekst)
        val standard = ObjectMother.saksbehandler(navIdent = "TEST-SAKSBEHANDLER")
        val annenIdent = ObjectMother.saksbehandler(navIdent = "TEST-BESLUTTER")
        val annenRolle = ObjectMother.beslutter(navIdent = "TEST-SAKSBEHANDLER")
        val flereRoller = ObjectMother.saksbehandler(
            navIdent = "TEST-BESLUTTER",
            roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER)),
        )
        checkNotNull(taRammebehandlinger(kontekst, listOf(sak.id to behandling.id), annenIdent))

        fun kommandoer(svar: JSONObject) =
            svar.getJSONArray("rammebehandlinger").getJSONObject(0).getJSONArray("gyldigeKommandoer").toString()

        suspend fun sammenlignMedSaksbehandler(query: String, saksbehandler: Saksbehandler): JSONObject {
            val forventet = checkNotNull(hentSakForSaksnummer(kontekst, sak.saksnummer, saksbehandler))
            val respons = client.get("/sak/${sak.saksnummer.verdi}$query") { bearerAuth("maskin") }
            respons.status shouldBe HttpStatusCode.OK
            return JSONObject(respons.bodyAsText()).also { JSONAssert.assertEquals(forventet, it, JSONCompareMode.STRICT) }
        }

        val logger = LoggerFactory.getLogger(MASKINOPPSLAG_LOGGER) as Logger
        val applogg = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(applogg)
        try {
            val standardsvar = sammenlignMedSaksbehandler("", standard)
            standardsvar.getJSONObject("fnr").getString("verdi") shouldBe sak.fnr.verdi
            standardsvar.getJSONObject("fnr").getBoolean("erSladdet") shouldBe false
            kommandoer(standardsvar) shouldContain "\"OvertaSaksbehandler\""
            kommandoer(sammenlignMedSaksbehandler("?testident=TEST-BESLUTTER", annenIdent)) shouldContain "\"Saksbehandle\""
            kommandoer(sammenlignMedSaksbehandler("?rolle=BESLUTTER", annenRolle)) shouldNotContain "\"OvertaSaksbehandler\""
            sammenlignMedSaksbehandler("?testident=TEST-BESLUTTER&rolle=SAKSBEHANDLER&rolle=BESLUTTER", flereRoller)

            applogg.list.size shouldBe 4
            applogg.list.forEach {
                it.level shouldBe Level.INFO
                it.formattedMessage shouldContain "Vellykket maskinoppslag: operasjon=saksoppslag, klientId=maskin-id"
                it.formattedMessage shouldNotContain sak.fnr.verdi
                it.formattedMessage shouldNotContain sak.saksnummer.verdi
            }
            val (standardlogg, identlogg, rollelogg, kombinasjonslogg) = applogg.list.map { it.formattedMessage }
            standardlogg shouldContain "testident=TEST-SAKSBEHANDLER, roller=[SAKSBEHANDLER]"
            identlogg shouldContain "testident=TEST-BESLUTTER, roller=[SAKSBEHANDLER]"
            rollelogg shouldContain "testident=TEST-SAKSBEHANDLER, roller=[BESLUTTER]"
            kombinasjonslogg shouldContain "testident=TEST-BESLUTTER, roller=["
            val loggedeRoller = kombinasjonslogg.substringAfter("roller=").removeSurrounding("[", "]").split(", ")
            loggedeRoller.toSet() shouldBe setOf("SAKSBEHANDLER", "BESLUTTER")
        } finally {
            logger.detachAppender(applogg)
            applogg.stop()
        }
    }

    @Test
    fun `ugyldige testaktørparametere gir 400 uten råverdier i svaret`() = medMaskinrute { kontekst ->
        val saksnummer = Saksnummer.genererSaknummer(clock = kontekst.clock, løpenr = "9999")
        val ugyldigeParametere = mapOf(
            "testident=ugyldig-verdi" to "ugyldig_testident",
            "testident=" to "ugyldig_testident",
            "testident=TEST-SAKSBEHANDLER&testident=TEST-BESLUTTER" to "ugyldig_testident",
            "rolle=ugyldig-verdi" to "ugyldig_rolle",
            "rolle=" to "ugyldig_rolle",
            "rolle" to "ugyldig_rolle",
        )
        ugyldigeParametere.forEach { (query, feilkode) ->
            val respons = client.get("/sak/${saksnummer.verdi}?$query") { bearerAuth("maskin") }
            respons.status shouldBe HttpStatusCode.BadRequest
            val svar = respons.bodyAsText()
            JSONObject(svar).getString("kode") shouldBe feilkode
            svar shouldNotContain "ugyldig-verdi"
        }
    }

    @Test
    fun `leserolle får sladdet sak selv med testparametere`() = medMaskinrute { kontekst ->
        val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = kontekst)
        kontekst.leggTilBruker("utvikler", ObjectMother.utvikler())
        val respons = client.get("/sak/${sak.saksnummer.verdi}?testident=TEST-SAKSBEHANDLER&rolle=SAKSBEHANDLER") {
            bearerAuth("utvikler")
        }
        respons.status shouldBe HttpStatusCode.OK
        val fnr = JSONObject(respons.bodyAsText()).getJSONObject("fnr")
        fnr.getBoolean("erSladdet") shouldBe true
        fnr.getString("verdi") shouldNotContain sak.fnr.verdi
    }

    @Test
    fun `gammel maskinsti gir 404 også i dev`() = medMaskinrute { kontekst ->
        val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = kontekst)
        client.get("/maskin/sak/${sak.saksnummer.verdi}") { bearerAuth("maskin") }.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `testparametere overstyrer ikke saksbehandlers persontilgang`() = medMaskinrute { kontekst ->
        val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = kontekst)
        val saksbehandler = ObjectMother.saksbehandler()
        kontekst.leggTilBruker("saksbehandler", saksbehandler)
        kontekst.tilgangsmaskinFakeClient.leggTil(
            sak.fnr,
            Tilgangsvurdering.Avvist(
                årsak = TilgangsvurderingAvvistÅrsak.SKJERMET,
                begrunnelse = "Tilgang er avvist",
                metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = saksbehandler.navIdent, brukerIdent = sak.fnr),
            ),
        )
        val respons = client.get("/sak/${sak.saksnummer.verdi}?testident=TEST-BESLUTTER&rolle=BESLUTTER") {
            bearerAuth("saksbehandler")
        }
        respons.status shouldBe HttpStatusCode.Forbidden
        respons.bodyAsText() shouldNotContain sak.fnr.verdi
    }
}

const val MASKINOPPSLAG_LOGGER = "no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.maskinoppslag.Maskinoppslag"

fun medMaskinrute(
    tilleggsruter: Route.(ApplicationContext) -> Unit = registrerUtviklingsruter,
    test: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    val texasClient = object : TexasClientFake(fixedClock) {
        override suspend fun introspectToken(token: String, identityProvider: IdentityProvider): TexasIntrospectionResponse {
            if (identityProvider != IdentityProvider.AZUREAD || token !in setOf("maskin", "maskin-uten-klient", "maskin-med-blank-klient")) {
                return super.introspectToken(token, identityProvider)
            }
            return TexasIntrospectionResponse(
                active = true,
                error = null,
                groups = null,
                roles = null,
                other = mutableMapOf(
                    "azp_name" to token,
                    "idtyp" to "app",
                ).apply {
                    when (token) {
                        "maskin" -> put("azp", "maskin-id")
                        "maskin-med-blank-klient" -> put("azp", " ")
                    }
                },
            )
        }
    }
    withTestApplicationContext(
        texasClient = texasClient,
        opprettSaksoppslagstilgang = ::utviklingstilgangForSaksoppslag,
        tilleggsruter = tilleggsruter,
        testBlock = test,
    )
}
