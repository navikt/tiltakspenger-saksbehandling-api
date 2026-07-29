package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbake

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbakeMeldekortbehandlingRoute]
 */
interface LeggTilbakeMeldekortbehandlingBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, JSONObject>? {
        val (sakMedOpprettetMeldekort, søknad, rammevedtakSøknadsbehandling, _, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
        ) ?: return null
        val meldekortId = sakMedOpprettetMeldekort.meldekortbehandlinger.first().id

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = leggTilbakeMeldekortbehandling(
            tac = tac,
            sakId = sakMedOpprettetMeldekort.id,
            meldekortId = meldekortId,
            saksbehandlerEllerBeslutter = saksbehandler,
            forventet = forventet,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortUnderBehandling,
            json,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgLeggTilbake(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingManuell, JSONObject>? {
        val (sakMedOpprettetMeldekort, søknad, rammevedtakSøknadsbehandling, _, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ) ?: return null
        val meldekortId = sakMedOpprettetMeldekort.meldekortbehandlinger.first().id

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = leggTilbakeMeldekortbehandling(
            tac = tac,
            sakId = sakMedOpprettetMeldekort.id,
            meldekortId = meldekortId,
            saksbehandlerEllerBeslutter = beslutter,
            forventet = forventet,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortbehandlingManuell,
            json,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BESLUTNING
     */
    suspend fun ApplicationTestBuilder.leggTilbakeMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerOgBeslutter"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Meldekortbehandling, JSONObject>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandlerEllerBeslutter,
        )
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutter)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/legg-tilbake",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            val jsonObject = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
