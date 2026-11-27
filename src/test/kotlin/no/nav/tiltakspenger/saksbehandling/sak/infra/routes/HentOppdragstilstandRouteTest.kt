package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dev.devRoutes
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentOppdragstilstandForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.migrerTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test

class HentOppdragstilstandRouteTest {

    @Test
    fun `henter oppdragstilstand for sak på oppdragsversjon 2`() {
        withTestApplicationContext(tilleggsruter = { devRoutes(it) }) { tac ->
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
        withTestApplicationContext(tilleggsruter = { devRoutes(it) }) { tac ->
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
        withTestApplicationContext(tilleggsruter = { devRoutes(it) }) { tac ->
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
        withTestApplicationContext(tilleggsruter = { devRoutes(it) }) { tac ->
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
        withTestApplicationContext(tilleggsruter = { devRoutes(it) }) {
            client.get("/sak/202501011001/oppdragstilstand").status shouldBe HttpStatusCode.Unauthorized
        }
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
