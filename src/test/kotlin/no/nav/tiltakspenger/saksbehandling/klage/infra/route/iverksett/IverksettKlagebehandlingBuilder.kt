package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import arrow.core.Tuple4
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagevedtakForKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortVedtakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.iverksettAvvistKlagebehandlingRoute]
 */
interface IverksettKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til avvisning
     *  3. Oppdaterer brevtekst
     *  4. Iverksetter
     */
    suspend fun ApplicationTestBuilder.opprettSakOgIverksettKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagevedtak, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, _) = this.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return iverksettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgKlagebehandlingTilAvvisning(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Meldekortvedtak, Klagevedtak, KlagebehandlingDTOJson>? {
        val (sak, meldekortVedtak, klagebehandling, _) = this.iverksettMeldekortVedtakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        val (sakEtterIverksettelse, klagevedtak, klagebehandlingJson) = iverksettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        ) ?: return null

        return Tuple4(
            sakEtterIverksettelse,
            meldekortVedtak,
            klagevedtak,
            klagebehandlingJson,
        )
    }

    /**
     * Forventer at det allerede finnes en sak.
     * Emulerer journalføring av vedtaksbrev.
     */
    suspend fun ApplicationTestBuilder.iverksettKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagevedtak, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/iverksett",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            // Emulerer journalføring og distribuering av vedtaksbrev, kun for denne saken.
            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførAvvisningbrevForSak(sakId)
            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerAvvisningsbrevForSak(sakId)
            val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(bodyAsText)
            val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagevedtakForKlagebehandlingId(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
