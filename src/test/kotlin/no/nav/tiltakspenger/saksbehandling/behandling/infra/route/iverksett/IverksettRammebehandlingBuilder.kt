package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface IverksettRammebehandlingBuilder {

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING` */
    suspend fun ApplicationTestBuilder.iverksettForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Tuple4<Sak, Rammevedtak, Rammebehandling, RammebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.leggTilBruker(jwt, beslutter)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/iverksett",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            if (medJsonBody != null) {
                medJsonBody(bodyAsText)
            }
            if (statusCode != 200) return null
            if (utførJobber) {
                // Emulerer jobbene som normalt ville blitt trigget av å sette behandling til IVERKSATT.
                tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
                tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
                tac.behandlingContext.journalførRammevedtaksbrevService.journalfør()
                tac.behandlingContext.distribuerRammevedtaksbrevService.distribuer()
            }
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val rammevedtak = sak.vedtaksliste.hentRammevedtakForBehandlingId(behandlingId)
            val rammebehandling = sak.rammebehandlinger.hentRammebehandling(behandlingId)!!
            return Tuple4(sak, rammevedtak, rammebehandling, JSONObject(bodyAsText))
        }
    }
}
