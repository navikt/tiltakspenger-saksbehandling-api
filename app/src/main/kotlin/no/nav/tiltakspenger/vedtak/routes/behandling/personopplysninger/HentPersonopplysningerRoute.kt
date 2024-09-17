package no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.parameter
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider

// TODO pre-mvp B: Midlertidig løsning for å ikke brekke dev. Denne skal skrives om til å hente personalia direkte fra pdl.
fun Route.hentPersonRoute(
    innloggetSaksbehandlerProvider: InnloggetSaksbehandlerProvider,
    sakService: SakService,
    auditService: AuditService,
) {
    get("$BEHANDLING_PATH/{behandlingId}/personopplysninger") {
        sikkerlogg.debug("Mottatt request på $BEHANDLING_PATH/{behandlingId}/personopplysninger")

        val saksbehandler = innloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(call)
        val behandlingId = BehandlingId.fromString(call.parameter("behandlingId"))

        val sak = sakService.hentForFørstegangsbehandlingId(behandlingId, saksbehandler)

        val personopplysninger = sak.personopplysninger.søker().toDTO()

        auditService.logMedBehandlingId(
            behandlingId = behandlingId,
            navIdent = saksbehandler.navIdent,
            action = AuditLogEvent.Action.ACCESS,
            contextMessage = "Henter personopplysninger for en behandling",
            callId = call.callId,
        )

        call.respond(status = HttpStatusCode.OK, personopplysninger)
    }
}
