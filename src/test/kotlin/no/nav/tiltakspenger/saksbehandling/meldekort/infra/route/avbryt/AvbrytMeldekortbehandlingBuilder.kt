package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt

import arrow.core.Tuple5
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.AvbruttMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbrytMeldekortBehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortBehandlingDTO]
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
        innvilgelsesperioder: List<Innvilgelsesperiode> = listOf(
            Innvilgelsesperiode(
                periode = vedtaksperiode,
                valgtTiltaksdeltakelse = tiltaksdeltakelse,
                antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, AvbruttMeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling, _) = this.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null
        val (oppdatertSak, avbruttMeldekortbehandling, json) = avbrytMeldekortBehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = opprettetMeldekortbehandling.id,
            begrunnelse = begrunnelse,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, AvbruttMeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val (_, opprettetMeldekortbehandling, _) = opprettMeldekortbehandlingForSakId(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
        ) ?: return null
        val (sakMedAvbruttMeldekortbehandling, avbruttMeldekortbehandling, json) = avbrytMeldekortBehandling(
            tac = tac,
            sakId = sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            begrunnelse = begrunnelse,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Triple(
            sakMedAvbruttMeldekortbehandling,
            avbruttMeldekortbehandling,
            json,
        )
    }

    suspend fun ApplicationTestBuilder.avbrytMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        begrunnelse: String = "begrunnelse for avbrytelse",
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, AvbruttMeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/avbryt")
            },
            jwt = jwt,
        ) {
            this.setBody("""{"begrunnelse":"$begrunnelse"}""")
        }.apply {
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
                oppdatertSak.hentMeldekortBehandling(meldekortId) as AvbruttMeldekortBehandling,
                jsonObject,
            )
        }
    }
}
