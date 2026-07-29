package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbrytMeldekortbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingDTO]
 */
interface AvbrytMeldekortbehandlingBuilder {

    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling
     * 3. Avbryter meldekortbehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(
        tac: TestApplicationContext,
        begrunnelse: String = "begrunnelse for avbrytelse",
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
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingAvbrutt, MeldekortbehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling, _) = this.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null
        val (oppdatertSak, avbruttMeldekortbehandling, json) = avbrytMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = opprettetMeldekortbehandling.id,
            begrunnelse = begrunnelse,
            saksbehandler = saksbehandler,
            forventet = forventet,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            avbruttMeldekortbehandling,
            json,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldeperioder som gir rett.
     * 1. Oppretter en meldekortbehandling
     * 2. Avbryter meldekortbehandlingen
     */
    suspend fun ApplicationTestBuilder.opprettOgAvbrytMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        begrunnelse: String = "begrunnelse for avbrytelse",
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortbehandlingAvbrutt, MeldekortbehandlingDTOJson>? {
        val (_, opprettetMeldekortbehandling, _) = opprettMeldekortbehandlingForSakId(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
        ) ?: return null
        val (sakMedAvbruttMeldekortbehandling, avbruttMeldekortbehandling, json) = avbrytMeldekortbehandling(
            tac = tac,
            sakId = sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            begrunnelse = begrunnelse,
            saksbehandler = saksbehandler,
            forventet = forventet,
        ) ?: return null
        return Triple(
            sakMedAvbruttMeldekortbehandling,
            avbruttMeldekortbehandling,
            json,
        )
    }

    suspend fun ApplicationTestBuilder.avbrytMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        begrunnelse: String = "begrunnelse for avbrytelse",
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortbehandlingAvbrutt, MeldekortbehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/avbryt",
            jwt = jwt,
            forventet = forventet,
            body = """{"begrunnelse":"$begrunnelse"}""",
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            val jsonObject: MeldekortbehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId) as MeldekortbehandlingAvbrutt,
                jsonObject,
            )
        }
    }
}
