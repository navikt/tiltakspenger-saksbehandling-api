package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import arrow.core.Tuple4
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
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import org.json.JSONObject
import java.time.LocalDate

interface StartRevurderingBuilder {

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            vedtaksperiode = søknadsbehandlingInnvilgelsesperiode,
            fnr = fnr,
            sakId = sakId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )
        val revurdering = startRevurderingForSakId(tac, sak.id, RevurderingType.STANS)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
        return Tuple4(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering!!,
        )
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingInnvilgelse(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingVedtaksperiode: Periode = søknadsbehandlingInnvilgelsesperiode.plusTilOgMed(14L),
        fnr: Fnr = Fnr.random(),
        sakId: SakId? = null,
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac,
            vedtaksperiode = søknadsbehandlingInnvilgelsesperiode,
            fnr = fnr,
            beslutter = beslutter,
            resultat = SøknadsbehandlingType.INNVILGELSE,
            sakId = sakId,
        )

        val tiltaksdeltakelseFakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient

        val oppdatertTiltaksdeltakelse =
            søknadsbehandling.saksopplysninger.getTiltaksdeltakelse(søknadsbehandling.søknad.tiltak!!.id)!!.copy(
                deltakelseFraOgMed = revurderingVedtaksperiode.fraOgMed,
                deltakelseTilOgMed = revurderingVedtaksperiode.tilOgMed,
            )

        tiltaksdeltakelseFakeKlient.lagre(
            sak.fnr,
            oppdatertTiltaksdeltakelse,
        )

        val revurdering = startRevurderingForSakId(tac, sak.id, RevurderingType.INNVILGELSE)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

        return Tuple4(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering!!,
        )
    }

    /**
     * Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering til omgjøring.
     * Default: Tiltaksdeltakelsen har endret seg fra 1. til 3. april.
     * @param oppdaterTiltaksdeltakelsesperiode Dersom null, fjernes den for dette fødselsnummeret.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1 til 10.april(2025),
        oppdaterTiltaksdeltakelsesperiode: Periode? = 3 til 10.april(2025),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering?> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            vedtaksperiode = søknadsbehandlingInnvilgelsesperiode,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            sakId = sakId,
            fnr = fnr,
        )
        val oppdatertTiltaksdeltakelse = if (oppdaterTiltaksdeltakelsesperiode != null) {
            søknadsbehandling.saksopplysninger
                .getTiltaksdeltakelse(søknadsbehandling.søknad.tiltak!!.id)!!.copy(
                deltakelseFraOgMed = oppdaterTiltaksdeltakelsesperiode.fraOgMed,
                deltakelseTilOgMed = oppdaterTiltaksdeltakelsesperiode.tilOgMed,
            )
        } else {
            null
        }
        tac.oppdaterTiltaksdeltakelse(sak.fnr, oppdatertTiltaksdeltakelse)

        val revurdering = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingType.OMGJØRING,
            rammevedtakIdSomOmgjøres = sak.vedtaksliste.single().id,
            forventetStatus = forventetStatus,
        )
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

        return Tuple4(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering,
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
    ): Revurdering? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.texasClient.leggTilBruker(jwt, saksbehandler)
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
                if (status != HttpStatusCode.OK) return null
                val revurderingId = BehandlingId.fromString(JSONObject(bodyAsText).getString("id"))
                return tac.behandlingContext.behandlingRepo.hent(revurderingId) as Revurdering
            }
    }
}
