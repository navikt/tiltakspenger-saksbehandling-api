package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Fake-klienten svarer alltid med én deltakelse fra 1. januar til 31. mars 2023.
 */
class HentTiltakdeltakelserRouteTest {

    @Test
    fun `deltakelser som overlapper oppslagsperioden returneres`() {
        withTestApplicationContext { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            hentTiltaksdeltakelser(tac, sak.id, "?fraOgMed=2023-03-01&tilOgMed=2023-04-30")
                .shouldContainJsonKeyValue("$[0].deltakelseFraOgMed", "2023-01-01")
        }
    }

    @Test
    fun `deltakelser utenfor oppslagsperioden filtreres bort`() {
        withTestApplicationContext { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            hentTiltaksdeltakelser(tac, sak.id, "?fraOgMed=2023-04-01&tilOgMed=2023-04-30") shouldEqualJson "[]"
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "?fraOgMed=2023-01-01", "?tilOgMed=2023-03-31", "?fraOgMed=ugyldig&tilOgMed=2023-03-31"])
    fun `manglende eller ugyldig oppslagsperiode gir 400`(query: String) {
        withTestApplicationContext { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            hentTiltaksdeltakelser(tac, sak.id, query, forventetStatus = 400)
                .shouldContainJsonKeyValue("kode", "oppslagsperiode_mangler")
        }
    }

    @Test
    fun `tilOgMed før fraOgMed gir 400`() {
        withTestApplicationContext { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            hentTiltaksdeltakelser(tac, sak.id, "?fraOgMed=2023-03-31&tilOgMed=2023-01-01", forventetStatus = 400)
                .shouldContainJsonKeyValue("kode", "negativ_periode")
        }
    }

    private suspend fun ApplicationTestBuilder.hentTiltaksdeltakelser(
        tac: TestApplicationContext,
        sakId: SakId,
        query: String,
        forventetStatus: Int = 200,
    ): String {
        val saksbehandler = ObjectMother.saksbehandler()
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.GET,
            "/sak/$sakId/tiltaksdeltakelser$query",
            jwt = jwt,
            forventet = ForventetRespons(forventetStatus, contentType = "application/json; charset=UTF-8"),
        ).body
    }
}
