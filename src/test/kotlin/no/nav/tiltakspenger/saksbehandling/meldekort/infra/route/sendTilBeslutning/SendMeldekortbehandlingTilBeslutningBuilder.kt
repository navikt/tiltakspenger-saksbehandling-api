package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.sendTilBeslutning

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.sendMeldekortTilBeslutningRoute]
 */
interface SendMeldekortbehandlingTilBeslutningBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        vedtaksperiode: Periode = 1.til(10.april(2025)),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(
            periode = vedtaksperiode,
            valgtTiltaksdeltakelse = tiltaksdeltakelse,
            antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
        ),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingManuell, MeldekortbehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, _, _) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
            tac = tac,
            fnr = fnr,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null
        val meldekortId = sak.meldekortbehandlinger.first().id
        val (sakMedMeldekortbehandling, meldekortUnderBehandling, json) = sendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            forventet = forventet,
        ) ?: return null

        return Tuple5(
            sakMedMeldekortbehandling,
            søknad,
            rammevedtakSøknadsbehandling,
            meldekortUnderBehandling,
            json,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BEHANDLING
     */
    suspend fun ApplicationTestBuilder.opprettOgSendMeldekortbehandlingTilBeslutning(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        meldeperioder: List<OppdatertMeldeperiodeDTO>? = null,
        skalSendeVedtaksbrev: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortbehandlingManuell, MeldekortbehandlingDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, meldekortbehandlingUnderBeslutning) = opprettOgOppdaterMeldekortbehandling(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            meldeperioder = meldeperioder,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        ) ?: return null
        return sendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandlingUnderBeslutning.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BEHANDLING
     */
    suspend fun ApplicationTestBuilder.sendMeldekortbehandlingTilBeslutning(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Triple<Sak, MeldekortbehandlingManuell, MeldekortbehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/sendtilbeslutning",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = body
            if (medJsonBody != null) {
                medJsonBody(bodyAsText)
            }
            if (statusCode != 200) return null
            val jsonObject: MeldekortbehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId) as MeldekortbehandlingManuell,
                jsonObject,
            )
        }
    }
}
