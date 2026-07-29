package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [avbrytKlagebehandlingRoute]
 */
interface AvbrytKlagebehandlingBuilder {
    /**
     * 1. Oppretter ny sak
     * 2. Starter klagebehandling til avvisning
     * 3. Avbryter
     */
    suspend fun ApplicationTestBuilder.avbruttKlagebehandlng(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, _) = this.opprettSakOgKlagebehandlingTilAvvisning(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return avbrytKlagebehandlingForSak(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og en åpen klagebehandling.
     * Merk at klagen ikke må være tilknyttet en åpen rammebehandling, da må den avbrytes først.
     */
    suspend fun ApplicationTestBuilder.avbrytKlagebehandlingForSak(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        avbruttStatus: AvbruttKlagebehandlingStatus = AvbruttKlagebehandlingStatus.ANNET,
        begrunnelse: String? = "begrunnelse for avbryt klagebehandling",
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val begrunnelseJson = if (begrunnelse != null) "\"$begrunnelse\"" else "null"
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/avbryt",
            jwt = jwt,
            forventet = forventet,
            body = """{"status": "$avbruttStatus", "begrunnelse": $begrunnelseJson}""",
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            val sakJson = objectMapper.readTree(bodyAsText)

            val klagebehandling: KlagebehandlingDTOJson = sakJson.get("klageBehandlinger").single {
                it.get("id").asString() == klagebehandlingId.toString()
            }

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                klagebehandling,
            )
        }
    }
}
