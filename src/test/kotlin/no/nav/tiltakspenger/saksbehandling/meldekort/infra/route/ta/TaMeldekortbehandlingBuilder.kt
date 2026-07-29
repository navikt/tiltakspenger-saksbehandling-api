package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.ta

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.taMeldekortbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingDTO]
 */
interface TaMeldekortbehandlingBuilder {

    /**
     * For saksbehandler.
     * Er aldri til beslutning.
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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling, _) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(
            tac = tac,
            saksbehandler = saksbehandlerSomOppretterOgLeggerTilbake,
        ) ?: return null

        val (sak, tattMeldekort, json) = taMeldekortbehanding(
            tac = tac,
            sakId = rammevedtakSøknadsbehandling.sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandlerSomTar,
            forventet = forventet,
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
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
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
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
            tac = tac,
            fnr = fnr,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null

        val (sak, tattMeldekort, json) = taMeldekortbehanding(
            tac = tac,
            sakId = rammevedtakSøknadsbehandling.sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            forventet = forventet,
        ) ?: return null
        return Tuple5(sak, søknad, rammevedtakSøknadsbehandling, tattMeldekort as MeldekortbehandlingManuell, json)
    }

    suspend fun ApplicationTestBuilder.opprettOgBesluttertarMeldekortbehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        meldeperioder: List<OppdatertMeldeperiodeDTO>? = null,
        skalSendeVedtaksbrev: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Meldekortbehandling, MeldekortbehandlingDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, meldekortbehandling) = opprettOgSendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            meldeperioder = meldeperioder,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            forventet = forventet,
        ) ?: return null

        return taMeldekortbehanding(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            forventet = forventet,
        )
    }

    /**
     * Fungerer både for saksbehandler og beslutter avhengig av hvilken status meldekortbehandlingen har før den tas.
     *
     * @return Dersom status går fra KLAR_TIL_BEHANDLING til UNDER_BEHANDLING returneres [MeldekortUnderBehandling].
     * Dersom status går fra KLAR_TIL_BESLUTNING til UNDER_BESLUTNING returneres [MeldekortbehandlingManuell]
     */
    suspend fun ApplicationTestBuilder.taMeldekortbehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Meldekortbehandling, MeldekortbehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandlerEllerBeslutter,
        )
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutter)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/ta",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            val jsonObject: MeldekortbehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
