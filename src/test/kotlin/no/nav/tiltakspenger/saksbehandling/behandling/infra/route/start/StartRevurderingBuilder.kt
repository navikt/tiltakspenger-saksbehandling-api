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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseFakeKlient
import org.json.JSONObject

interface StartRevurderingBuilder {

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingStans(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        virkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            virkningsperiode = virkningsperiode,
        )
        val revurdering = startRevurderingForSakId(tac, sak.id, RevurderingType.STANS)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
        return Tuple4(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering,
        )
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingInnvilgelse(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        søknadsbehandlingVirkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        revurderingVirkningsperiode: Periode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac,
            virkningsperiode = søknadsbehandlingVirkningsperiode,
        )

        val tiltaksdeltagelseFakeKlient =
            tac.tiltakContext.tiltaksdeltagelseKlient as TiltaksdeltagelseFakeKlient

        val oppdatertTiltaksdeltagelse =
            søknadsbehandling.saksopplysninger.getTiltaksdeltagelse(søknadsbehandling.søknad.tiltak!!.id)!!.copy(
                deltagelseFraOgMed = revurderingVirkningsperiode.fraOgMed,
                deltagelseTilOgMed = revurderingVirkningsperiode.tilOgMed,
            )

        tiltaksdeltagelseFakeKlient.lagre(
            sak.fnr,
            oppdatertTiltaksdeltagelse,
        )

        val revurdering = startRevurderingForSakId(tac, sak.id, RevurderingType.INNVILGELSE)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

        return Tuple4(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering,
        )
    }

    /**
     * Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering til omgjøring.
     * Default: Tiltaksdeltagelsen har endret seg fra 1. til 3. april.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingOmgjøring(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        søknadsbehandlingVirkningsperiode: Periode = 1 til 10.april(2025),
        oppdaterTiltaksdeltagelsesperiode: Periode = 3 til 10.april(2025),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac,
            virkningsperiode = søknadsbehandlingVirkningsperiode,
        )
        val oppdatertTiltaksdeltagelse = søknadsbehandling.saksopplysninger
            .getTiltaksdeltagelse(søknadsbehandling.søknad.tiltak!!.id)!!.copy(
            deltagelseFraOgMed = oppdaterTiltaksdeltagelsesperiode.fraOgMed,
            deltagelseTilOgMed = oppdaterTiltaksdeltagelsesperiode.tilOgMed,
        )
        tac.oppdaterTiltaksdeltagelse(sak.fnr, oppdatertTiltaksdeltagelse)

        val revurdering = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingType.OMGJØRING,
            rammevedtakIdSomOmgjøres = sak.vedtaksliste.single().id,
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
    ): Revurdering {
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
                    status shouldBe HttpStatusCode.OK
                }
                val revurderingId = BehandlingId.fromString(JSONObject(bodyAsText).getString("id"))
                return tac.behandlingContext.behandlingRepo.hent(revurderingId) as Revurdering
            }
    }
}
