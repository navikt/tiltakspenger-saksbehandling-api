package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import arrow.core.Tuple5
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

interface StartRevurderingBuilder {

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperioder.totalPeriode),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetillegg,
            tiltaksdeltakelse = tiltaksdeltakelse,
            fnr = fnr,
            sakId = sakId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )
        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingType.STANS,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )!!
        return Tuple5(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    @Suppress("unused")
    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingType.STANS,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse? = tiltaksdeltakelse(søknadsbehandlingInnvilgelsesperioder.totalPeriode),
        fnr: Fnr = Fnr.random(),
        sakId: SakId? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson> {
        val (sak, søknad, rammevedtakSøknadsbehandling) = iverksettSøknadsbehandling(
            tac,
            fnr = fnr,
            beslutter = beslutter,
            resultat = SøknadsbehandlingType.INNVILGELSE,
            sakId = sakId,
            innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        )

        val tiltaksdeltakelseFakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient

        tiltaksdeltakelseFakeKlient.lagre(
            sak.fnr,
            oppdatertTiltaksdeltakelse,
        )

        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac,
            sak.id,
            RevurderingType.INNVILGELSE,
        )!!

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    /**
     * Starter en ny revurdering til innvilgelse på [sakId]
     * Merk at denne ikke oppretter sak, søknad eller søknadsbehandling.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingType.INNVILGELSE,
            saksbehandler = saksbehandler,
            rammevedtakIdSomOmgjøres = null,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /**
     * Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering til omgjøring.
     * Default: Tiltaksdeltakelsen har endret seg fra 1. til 3. april.
     * @param [oppdatertTiltaksdeltakelse] Dersom null, fjernes den for dette fødselsnummeret.
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse? = søknadsbehandlingInnvilgelsesperioder.periodisering.first().verdi.valgtTiltaksdeltakelse,
        forventetStatusForStartRevurdering: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBodyForStartRevurdering: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            sakId = sakId,
            fnr = fnr,
            innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        )

        tac.oppdaterTiltaksdeltakelse(sak.fnr, oppdatertTiltaksdeltakelse)

        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingType.OMGJØRING,
            rammevedtakIdSomOmgjøres = sak.rammevedtaksliste.single().id,
            forventetStatus = forventetStatusForStartRevurdering,
            forventetJsonBody = forventetJsonBodyForStartRevurdering,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    /**
     * Starter en ny revurdering til omgjøring på [sakId]
     * Merk at denne ikke oppretter sak, søknad eller søknadsbehandling.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId,
        rammevedtakIdSomOmgjøres: VedtakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingType.OMGJØRING,
            rammevedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
        )
    }

    /** Forventer at det allerede finnes en sak og søknad. */
    suspend fun ApplicationTestBuilder.startRevurderingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        type: RevurderingType,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        rammevedtakIdSomOmgjøres: VedtakId? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/revurdering/start")
            },
            jwt = jwt,
        ) {
            setBody(
                """
                {
                "revurderingType": "${type.tilDTO()}", 
                "rammevedtakIdSomOmgjøres": ${if (rammevedtakIdSomOmgjøres != null) """"$rammevedtakIdSomOmgjøres"""" else null}
                }
                """.trimIndent(),
            )
        }
            .apply {
                val bodyAsText = this.bodyAsText()
                withClue(
                    "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                ) {
                    if (forventetStatus != null) status shouldBe forventetStatus
                }

                if (forventetJsonBody != null) {
                    bodyAsText.shouldEqualJson(forventetJsonBody)
                }
                if (status != HttpStatusCode.OK) return null
                val jsonObject: RammebehandlingDTOJson = JSONObject(bodyAsText)
                val revurderingId = BehandlingId.fromString(jsonObject.getString("id"))
                val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
                return Triple(
                    oppdatertSak,
                    tac.behandlingContext.behandlingRepo.hent(revurderingId) as Revurdering,
                    jsonObject,
                )
            }
    }
}
