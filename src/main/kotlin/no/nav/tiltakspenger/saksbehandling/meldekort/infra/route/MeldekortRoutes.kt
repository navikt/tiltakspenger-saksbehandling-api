package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GjenopptaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OvertaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortbehandlingTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortbehandlingService
import java.time.Clock

fun Route.meldekortRoutes(
    opprettMeldekortbehandlingService: OpprettMeldekortbehandlingService,
    iverksettMeldekortbehandlingService: IverksettMeldekortbehandlingService,
    oppdaterMeldekortbehandlingService: OppdaterMeldekortbehandlingService,
    auditService: AuditService,
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
    underkjennMeldekortbehandlingService: UnderkjennMeldekortbehandlingService,
    overtaMeldekortbehandlingService: OvertaMeldekortbehandlingService,
    taMeldekortbehandlingService: TaMeldekortbehandlingService,
    leggTilbakeMeldekortbehandlingService: LeggTilbakeMeldekortbehandlingService,
    sendMeldekortbehandlingTilBeslutterService: SendMeldekortbehandlingTilBeslutterService,
    avbrytMeldekortbehandlingService: AvbrytMeldekortbehandlingService,
    settMeldekortbehandlingPåVentService: SettMeldekortbehandlingPåVentService,
    gjenopptaMeldekortbehandlingService: GjenopptaMeldekortbehandlingService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
    forhåndsvisBrevMeldekortbehandlingService: ForhåndsvisBrevMeldekortbehandlingService,
) {
    iverksettMeldekortRoute(iverksettMeldekortbehandlingService, auditService, clock, tilgangskontrollService)
    sendMeldekortTilBeslutterRoute(sendMeldekortbehandlingTilBeslutterService, auditService, clock, tilgangskontrollService)
    oppdaterMeldekortbehandlingRoute(oppdaterMeldekortbehandlingService, auditService, clock, tilgangskontrollService)
    opprettMeldekortbehandlingRoute(opprettMeldekortbehandlingService, auditService, clock, tilgangskontrollService)
    overtaMeldekortbehandlingRoute(overtaMeldekortbehandlingService, auditService, tilgangskontrollService)
    mottaMeldekortRoute(mottaBrukerutfyltMeldekortService)
    taMeldekortbehandlingRoute(auditService, taMeldekortbehandlingService, tilgangskontrollService)
    underkjennMeldekortbehandlingRoute(underkjennMeldekortbehandlingService, auditService, tilgangskontrollService)
    leggTilbakeMeldekortbehandlingRoute(auditService, leggTilbakeMeldekortbehandlingService, tilgangskontrollService)
    avbrytMeldekortbehandlingRoute(auditService, avbrytMeldekortbehandlingService, tilgangskontrollService)
    settMeldekortbehandlingPåVentRoute(auditService, settMeldekortbehandlingPåVentService, tilgangskontrollService)
    gjenopptaMeldekortbehandlingRoute(auditService, gjenopptaMeldekortbehandlingService, tilgangskontrollService)
    forhåndsvisBrevMeldekortbehandlingRoute(forhåndsvisBrevMeldekortbehandlingService, auditService, tilgangskontrollService)
}
