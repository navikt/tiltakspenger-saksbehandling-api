package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface OppdaterRammebehandlingSaksopplysningerBuilder {

    /**
     * Forventer at det allerede finnes en behandling.
     * Denne fungerer både for saksbehandler og beslutter.
     */
    suspend fun ApplicationTestBuilder.oppdaterSaksopplysningerForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Rammebehandling, String> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/behandling/$behandlingId/saksopplysninger",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }
}
