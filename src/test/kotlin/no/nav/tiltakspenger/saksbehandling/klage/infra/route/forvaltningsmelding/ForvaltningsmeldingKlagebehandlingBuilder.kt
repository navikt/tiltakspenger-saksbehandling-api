package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak

val standardForvaltningsmeldingtekster: List<Pair<String, String>> = listOf(
    "Svartid" to "Saken vil bli behandlet innen ca. 3 måneder.",
    "Begrunnelse for saksbehandlingstiden vår" to "Vi har mange saker til behandling.",
)

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding.sendForvaltningsmeldingRoute] og [no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding.forhåndsvisForvaltningsmeldingRoute]
 */
interface ForvaltningsmeldingKlagebehandlingBuilder {

    /**
     * 1. Oppretter ny sak med iverksatt søknadsbehandling
     * 2. Starter klagebehandling til opprettholdelse
     * 3. Sender forvaltningsmelding
     */
    suspend fun ApplicationTestBuilder.opprettSakOgSendForvaltningsmelding(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        tekster: List<Pair<String, String>> = standardForvaltningsmeldingtekster,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, _, klagebehandling, _) = this.opprettSakOgKlagebehandlingTilOpprettholdelse(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return sendForvaltningsmelding(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            tekster = tekster,
            forventet = forventet,
        )
    }

    /** Forventer at det allerede finnes en sak og klagebehandling. */
    suspend fun ApplicationTestBuilder.sendForvaltningsmelding(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        tekster: List<Pair<String, String>> = standardForvaltningsmeldingtekster,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/klage/$klagebehandlingId/forvaltningsmelding",
            jwt = jwt,
            forventet = forventet,
            body = tekster.tilForvaltningsmeldingBody(),
        ).apply {
            if (statusCode != 200) return null
            val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(this.body)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }

    /** Forventer at det allerede finnes en sak og klagebehandling. */
    suspend fun ApplicationTestBuilder.forhåndsvisForvaltningsmelding(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        tekster: List<Pair<String, String>> = standardForvaltningsmeldingtekster,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/pdf"),
    ): PdfA? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/klage/$klagebehandlingId/forvaltningsmelding/forhandsvis",
            jwt = jwt,
            forventet = forventet,
            body = tekster.tilForvaltningsmeldingBody(),
        )
        if (response.statusCode != 200) return null
        return PdfA(response.bytes)
    }
}

private fun List<Pair<String, String>>.tilForvaltningsmeldingBody(): String = serialize(
    mapOf("tekster" to this.map { (tittel, tekst) -> mapOf("tittel" to tittel, "tekst" to tekst) }),
)
