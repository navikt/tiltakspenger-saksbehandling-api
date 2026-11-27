package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dev.registrerUtviklingsruter
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.oppdragstilstand.hentOppdragstilstandRoute
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentOppdragstilstandForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.migrerTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.LoggerFactory

@Isolated("Fanger hendelser fra den felles auditloggeren.")
class HentOppdragstilstandRouteTest {

    @Test
    fun `henter oppdragstilstand for sak på oppdragsversjon 2`() {
        withTestApplicationContext(tilleggsruter = registrerUtviklingsruter) { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac)

            val oppdragstilstandJson = hentOppdragstilstandForSaksnummer(
                tac = tac,
                saksnummer = sak.saksnummer,
            ).shouldNotBeNull()

            oppdragstilstandJson.getString("sakId") shouldBe sak.id.toString()
            oppdragstilstandJson.getString("saksnummer") shouldBe sak.saksnummer.verdi
            oppdragstilstandJson.getString("oppdragsversjon") shouldBe "V2"
            oppdragstilstandJson.isNull("oppdragsmigrering") shouldBe true
            oppdragstilstandJson.getBoolean("sperrerUtbetalinger") shouldBe false
            oppdragstilstandJson.isNull("migrertHosHelVedTidspunkt") shouldBe true
        }
    }

    @Test
    fun `henter oppdragstilstand for sak som er migrert til oppdragsversjon 3`() {
        withTestApplicationContext(tilleggsruter = registrerUtviklingsruter) { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac)
            migrerTilOppdragsversjon3(tac = tac, sakId = sak.id)

            val oppdragstilstandJson = hentOppdragstilstandForSaksnummer(
                tac = tac,
                saksnummer = sak.saksnummer,
            ).shouldNotBeNull()

            oppdragstilstandJson.getString("sakId") shouldBe sak.id.toString()
            oppdragstilstandJson.getString("saksnummer") shouldBe sak.saksnummer.verdi
            oppdragstilstandJson.getString("oppdragsversjon") shouldBe "V3"
            oppdragstilstandJson.getString("oppdragsmigrering") shouldBe "MIGRERT_HOS_HEL_VED"
            oppdragstilstandJson.getBoolean("sperrerUtbetalinger") shouldBe true
            oppdragstilstandJson.isNull("migrertHosHelVedTidspunkt") shouldBe false
        }
    }

    @Test
    fun `ukjent saksnummer gir 404`() {
        withTestApplicationContext(tilleggsruter = registrerUtviklingsruter) { tac ->
            val ukjentSaksnummer = Saksnummer.genererSaknummer(clock = tac.clock, løpenr = "9999")

            hentOppdragstilstandForSaksnummer(
                tac = tac,
                saksnummer = ukjentSaksnummer,
                forventet = ForventetRespons(status = 404),
            ) shouldBe null
        }
    }

    @Test
    fun `nektet persontilgang gir 403`() {
        withTestApplicationContext(tilleggsruter = registrerUtviklingsruter) { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac)
            tac.tilgangsmaskinFakeClient.leggTil(
                sak.fnr,
                Tilgangsvurdering.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.SKJERMET,
                    begrunnelse = "Tilgang er avvist",
                    metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = "test", brukerIdent = sak.fnr),
                ),
            )

            hentOppdragstilstandForSaksnummer(
                tac = tac,
                saksnummer = sak.saksnummer,
                forventet = ForventetRespons(403),
            ) shouldBe null
        }
    }

    @Test
    fun `kall uten token gir 401`() {
        withTestApplicationContext(tilleggsruter = registrerUtviklingsruter) {
            client.get("/sak/202501011001/oppdragstilstand").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `dev krever verifisert maskintoken med klient-ID og gyldige parametre`() = medMaskinrute { kontekst ->
        val saksnummer = Saksnummer.genererSaknummer(clock = kontekst.clock, løpenr = "9999")
        val sti = "/sak/$saksnummer/oppdragstilstand"
        client.get(sti) { bearerAuth("ugyldig-token") }.status shouldBe HttpStatusCode.Unauthorized
        listOf("maskin-uten-klient", "maskin-med-blank-klient").forEach { token ->
            client.get(sti) { bearerAuth(token) }.status shouldBe HttpStatusCode.Forbidden
        }
        listOf("testident=ugyldig", "rolle=", "rolle=ugyldig").forEach { parametre ->
            client.get("$sti?$parametre") { bearerAuth("maskin") }.status shouldBe HttpStatusCode.BadRequest
        }
        client.get("/sak/ugyldig/oppdragstilstand") { bearerAuth("maskin") }.status shouldBe HttpStatusCode.BadRequest
        val ukjentSak = client.get(sti) { bearerAuth("maskin") }
        ukjentSak.status shouldBe HttpStatusCode.NotFound
        ukjentSak.bodyAsText() shouldBe "Fant ikke sak med saksnummer $saksnummer"
    }

    @Test
    fun `dev-maskin får samme svar uten persontilgang og logges med faktisk klient-ID`() = medMaskinrute { kontekst ->
        val (sak) = opprettSakOgSøknad(tac = kontekst)
        val saksbehandler = ObjectMother.saksbehandler()
        kontekst.leggTilBruker("saksbehandler", saksbehandler)
        val sti = "/sak/${sak.saksnummer}/oppdragstilstand"
        val maskinlogger = LoggerFactory.getLogger(MASKINOPPSLAG_LOGGER) as Logger
        val auditlogger = LoggerFactory.getLogger("audit") as Logger
        val applogg = ListAppender<ILoggingEvent>().apply { start() }
        val auditlogg = ListAppender<ILoggingEvent>().apply { start() }
        maskinlogger.addAppender(applogg)
        auditlogger.addAppender(auditlogg)
        try {
            val saksbehandlersvar = client.get(sti) { bearerAuth("saksbehandler") }
            saksbehandlersvar.status shouldBe HttpStatusCode.OK
            val forventetSvar = saksbehandlersvar.bodyAsText()
            kontekst.tilgangsmaskinFakeClient.leggTil(
                sak.fnr,
                Tilgangsvurdering.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.SKJERMET,
                    begrunnelse = "Tilgang er avvist",
                    metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = saksbehandler.navIdent, brukerIdent = sak.fnr),
                ),
            )
            listOf("", "?testident=TEST-BESLUTTER&rolle=BESLUTTER").forEach { parametre ->
                val svar = client.get("$sti$parametre") { bearerAuth("maskin") }
                svar.status shouldBe HttpStatusCode.OK
                svar.bodyAsText() shouldBe forventetSvar
            }
            applogg.list.map { it.level } shouldBe listOf(Level.INFO, Level.INFO)
            val (standardlogg, overstyrtLogg) = applogg.list.map { it.formattedMessage }
            standardlogg shouldContain "operasjon=oppdragstilstand, klientId=maskin-id, testident=TEST-SAKSBEHANDLER, roller=[SAKSBEHANDLER]"
            overstyrtLogg shouldContain "operasjon=oppdragstilstand, klientId=maskin-id, testident=TEST-BESLUTTER, roller=[BESLUTTER]"
            applogg.list.forEach {
                it.formattedMessage shouldNotContain sak.fnr.verdi
                it.formattedMessage shouldNotContain sak.saksnummer.verdi
                it.formattedMessage shouldNotContain sak.id.toString()
            }
            val oppdragsaudit = auditlogg.list.map { it.formattedMessage }.filter { "msg=Henter oppdragstilstand for saken" in it }
            oppdragsaudit.size shouldBe 3
            oppdragsaudit.zip(listOf(saksbehandler.navIdent, "maskin-id", "maskin-id")).forEach { (melding, ident) ->
                melding shouldContain " suid=$ident "
                melding shouldContain " duid=${sak.fnr.verdi} "
            }
        } finally {
            maskinlogger.detachAppender(applogg)
            auditlogger.detachAppender(auditlogg)
            applogg.stop()
            auditlogg.stop()
        }
    }

    @Test
    fun `lokal standardregistrering avviser maskin og støtter saksbehandler`() = medMaskinrute(
        tilleggsruter = { kontekst ->
            hentOppdragstilstandRoute(
                sakService = kontekst.sakContext.sakService,
                utbetalingRepo = kontekst.utbetalingContext.utbetalingRepo,
                auditService = kontekst.personContext.auditService,
                tilgangskontrollService = kontekst.tilgangskontrollService,
            )
        },
    ) { kontekst ->
        val (sak) = opprettSakOgSøknad(tac = kontekst)
        val sti = "/sak/${sak.saksnummer}/oppdragstilstand"
        kontekst.leggTilBruker("saksbehandler", ObjectMother.saksbehandler())
        client.get(sti) { bearerAuth("maskin") }.status shouldBe HttpStatusCode.Forbidden
        client.get(sti) { bearerAuth("saksbehandler") }.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `produksjonsappen har ikke oppdragstilstand-ruten uten dev-rutene`() {
        var harOppdragstilstand: Boolean? = null
        withTestApplicationContext(
            additionalConfig = { harOppdragstilstand = routing {}.harPathSegment("oppdragstilstand") },
        ) {
            startApplication()
        }
        harOppdragstilstand shouldBe false
    }

    private fun Route.harPathSegment(pathSegment: String): Boolean {
        return selector.toString() == pathSegment || children.any { it.harPathSegment(pathSegment) }
    }
}
