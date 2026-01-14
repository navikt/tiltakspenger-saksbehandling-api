package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

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
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URLEncoder.encode

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprettMeldekortBehandlingRoute]
 */
interface OpprettMeldekortbehandlingBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
        tac: TestApplicationContext,
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
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val (sak, søknad, rammevedtak, _) = iverksettSøknadsbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        )
        val førsteMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
        val (sakMedMeldekortbehandling, meldekortbehandling, opprettMeldekortbehandlingResponse) = opprettMeldekortbehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            kjedeId = førsteMeldeperiode.kjedeId,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(
            sakMedMeldekortbehandling,
            søknad,
            rammevedtak,
            meldekortbehandling,
            opprettMeldekortbehandlingResponse,
        )
    }

    /** Forventer at det det finnes en sak med en meldeperiode som gir rett til tiltakspenger */
    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val kjedeId = "${kjedeId.fraOgMed}%2F${kjedeId.tilOgMed}"
        defaultRequest(
            HttpMethod.Post,
            "/sak/$sakId/meldeperiode/$kjedeId/opprettBehandling",
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                if (forventetStatus != null) status shouldBe forventetStatus
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val meldekortbehandlingerJson = jsonObject.getJSONArray("meldekortBehandlinger")
            val meldekortbehandlingJson =
                meldekortbehandlingerJson.getJSONObject(meldekortbehandlingerJson.length() - 1)
            val meldekortbehandlingId = MeldekortId.fromString(meldekortbehandlingJson.getString("id"))
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortId = meldekortbehandlingId) as MeldekortUnderBehandling,
                jsonObject,
            )
        }
    }
}
