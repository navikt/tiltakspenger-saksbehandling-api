package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.gjenoppta

import arrow.core.Tuple5
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.settPåVent.SettMeldekortbehandlingPåVentBuilder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.gjenopptaMeldekortbehandlingRoute]
 */
interface GjenopptaMeldekortbehandlingBuilder : SettMeldekortbehandlingPåVentBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingSettPåVentOgGjenoppta(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, meldekortbehandling, _) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(
            tac = tac,
            saksbehandler = saksbehandler,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = gjenopptaMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortUnderBehandling,
            json,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingSettPåVentOgGjenoppta(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingManuell, SakDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, meldekortbehandling, _) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = gjenopptaMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
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
     * Forventer at meldekortbehandlingen er satt på vent.
     */
    suspend fun ApplicationTestBuilder.gjenopptaMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerOgBeslutter"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Meldekortbehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandlerEllerBeslutter,
        )
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutter)
        defaultRequestWithAssertions(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/gjenoppta")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { status ->
                ForventetRespons(
                    status = status,
                    body = forventetJsonBody?.let { ForventetBody.Json(it) },
                    contentType = ContentType.parse("application/json; charset=UTF-8"),
                )
            },
        ).apply {
            val bodyAsText = bodyAsText()
            if (status != HttpStatusCode.OK) return null
            val jsonObject: SakDTOJson = objectMapper.readTree(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
