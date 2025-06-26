package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
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
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface IverksettBehandlingBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
            PeriodeMedVerdi(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkingsperiode,
            ),
        ),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(
            tac = tac,
            fnr = fnr,
            virkingsperiode = virkingsperiode,
            resultat = resultat,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        )
        taBehanding(tac, sak.id, behandlingId, beslutter)
        val (oppdatertSak, oppdatertBehandling, jsonResponse) = iverksettForBehandlingId(
            tac,
            sak.id,
            behandlingId,
            beslutter,
        )
        return Tuple4(
            oppdatertSak,
            søknad,
            oppdatertBehandling as Søknadsbehandling,
            jsonResponse,
        )
    }

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.iverksettAutomatiskBehandletSøknadsbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
            PeriodeMedVerdi(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkingsperiode,
            ),
        ),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad, behandling, _) = opprettAutomatiskBehandlingKlarTilBeslutning(
            tac = tac,
            fnr = fnr,
            virkingsperiode = virkingsperiode,
        )
        taBehanding(tac, sak.id, behandling.id, beslutter)
        val (oppdatertSak, oppdatertBehandling, jsonResponse) = iverksettForBehandlingId(
            tac,
            sak.id,
            behandling.id,
            beslutter,
        )
        return Tuple4(
            oppdatertSak,
            søknad,
            oppdatertBehandling as Søknadsbehandling,
            jsonResponse,
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING` */
    suspend fun ApplicationTestBuilder.iverksettForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): Triple<Sak, Behandling, String> {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/iverksett")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = beslutter,
            ),
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = sak.behandlinger.hentBehandling(behandlingId)!!
            return Triple(sak, behandling, bodyAsText)
        }
    }

    suspend fun ApplicationTestBuilder.iverksettForBehandlingIdReturnerRespons(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): HttpResponse {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/iverksett")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = beslutter,
            ),
        ).apply {
            return this
        }
    }
}
