package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlePåNytt

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import org.json.JSONObject

interface BehandleSøknadPåNyttBuilder {
    /** Forventer at det finnes en avslått søknadsbehandling for søknaden. Oppretter ny søknadsbehandling */
    suspend fun ApplicationTestBuilder.behandleSøknadPåNytt(
        tac: TestApplicationContext,
        sak: Sak,
        søknad: Søknad,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (behandling, response) = startBehandlingAvSøknadPåyttForSøknadId(tac, sak.id, søknad.id)
        return Tuple4(sak, søknad, behandling as Søknadsbehandling, response)
    }

    /** Forventer at det allerede finnes en sak og søknad. */
    suspend fun ApplicationTestBuilder.startBehandlingAvSøknadPåyttForSøknadId(
        tac: TestApplicationContext,
        sakId: SakId,
        søknadId: SøknadId,
    ): Pair<Behandling, String> {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/soknad/$søknadId/behandling/ny-behandling")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {}
            val behandlingId = BehandlingId.fromString(JSONObject(bodyAsText).getString("id"))
            return tac.behandlingContext.behandlingRepo.hent(behandlingId) to bodyAsText
        }
    }
}
