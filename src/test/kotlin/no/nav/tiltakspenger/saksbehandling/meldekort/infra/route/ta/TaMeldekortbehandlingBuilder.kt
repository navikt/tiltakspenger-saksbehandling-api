package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.ta

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
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortBehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.taMeldekortBehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortBehandlingDTO]
 */
interface TaMeldekortbehandlingBuilder {

    /**
     * For saksbehandler. Er aldri til beslutning.
     *
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling med saksbehandler 1 (UNDER_BEHANDLING)
     * 3. Saksbehandler 1 legger tilbake (KLAR_TIL_BEHANDLING).
     * 4. Saksbehandler 2 tar meldekortbehandlingen (UNDER_BEHANDLING)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehadlingLeggTilbakeOgTaMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandlerSomOppretterOgLeggerTilbake: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOppretterOgLeggerTilbake"),
        saksbehandlerSomTar: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldekortBehandlingDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortBehandling, _) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(
            tac = tac,
            saksbehandler = saksbehandlerSomOppretterOgLeggerTilbake,
        ) ?: return null

        val (sak, tattMeldekort, json) = taMeldekortbehanding(
            tac = tac,
            sakId = rammevedtakSøknadsbehandling.sakId,
            meldekortId = opprettetMeldekortBehandling.id,
            saksbehandlerEllerBeslutter = saksbehandlerSomTar,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(sak, søknad, rammevedtakSøknadsbehandling, tattMeldekort as MeldekortUnderBehandling, json)
    }

    /**
     * For beslutter.
     *
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling (UNDER_BEHANDLING)
     * 3. Oppdaterer meldekortbehandlingen slik at den er klar for å sendes til beslutning.
     * 4. Saksbehandler sender til beslutning.
     * 5. Beslutter tar behandling.
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        vedtaksperiode: Periode = 1.til(10.april(2025)),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        innvilgelsesperioder: List<Innvilgelsesperiode> = listOf(
            Innvilgelsesperiode(
                periode = vedtaksperiode,
                valgtTiltaksdeltakelse = tiltaksdeltakelse,
                antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortBehandletManuelt, MeldekortBehandlingDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortBehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
            tac = tac,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null

        val (sak, tattMeldekort, json) = taMeldekortbehanding(
            tac = tac,
            sakId = rammevedtakSøknadsbehandling.sakId,
            meldekortId = opprettetMeldekortBehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(sak, søknad, rammevedtakSøknadsbehandling, tattMeldekort as MeldekortBehandletManuelt, json)
    }

    suspend fun ApplicationTestBuilder.opprettOgBesluttertarMeldekortbehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, meldekortbehandling) = opprettOgSendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null

        return taMeldekortbehanding(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /**
     * Fungerer både for saksbehandler og beslutter avhengig av hvilken status meldekortbehandlingen har før den tas.
     *
     * @return Dersom status går fra KLAR_TIL_BEHANDLING til UNDER_BEHANDLING returneres [MeldekortUnderBehandling]. Dersom status går fra KLAR_TIL_BESLUTNING til UNDER_BESLUTNING returneres [no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt]
     */
    suspend fun ApplicationTestBuilder.taMeldekortbehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandlerEllerBeslutter,
        )
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutter)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/ta")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldekortBehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortBehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
