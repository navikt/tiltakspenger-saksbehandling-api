package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjenn

import arrow.core.Tuple5
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjennMeldekortbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingDTO]
 */
interface UnderkjennMeldekortbehandlingBuilder {
    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling
     * 3. Oppdaterer behandlingen
     * 4. Sender til beslutning
     * 5. Beslutter tar behandlingen.
     * 6. Beslutter underkjenner.
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling(
        tac: TestApplicationContext,
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for underkjennelse",
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, søknad, rammevedtakSøknadsbehandling, meldekortbehandlingUnderBeslutning) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            beslutter = beslutter,
        ) ?: return null
        val (oppdatertSak, oppdatertMeldekort, json) = underkjennMeldekortbehandling(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandlingUnderBeslutning.id,
            beslutter = beslutter,
            forventet = forventet,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekort,
            json,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BESLUTNING
     */
    suspend fun ApplicationTestBuilder.underkjennMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for underkjennelse",
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.leggTilBruker(jwt, beslutter)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/underkjenn",
            jwt = jwt,
            forventet = forventet,
            body = """{"begrunnelse": "$begrunnelse"}""",
        ).apply {
            val bodyAsText = body
            if (statusCode != 200) return null
            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val meldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortId) as MeldekortUnderBehandling
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            meldekortbehandling.attesteringer.single().also {
                it.beslutter shouldBe beslutter.navIdent
                it.begrunnelse!!.value shouldBe begrunnelse
                it.status shouldBe Attesteringsstatus.SENDT_TILBAKE
            }
            return Triple(
                oppdatertSak,
                meldekortbehandling,
                jsonObject,
            )
        }
    }
}
