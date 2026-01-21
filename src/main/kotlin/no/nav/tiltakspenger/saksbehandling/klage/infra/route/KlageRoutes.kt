package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.forhåndsvisBrevKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.iverksettKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.oppdaterTekstTilBrev
import no.nav.tiltakspenger.saksbehandling.klage.service.AvbrytKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.ForhåndsvisBrevKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.IverksettKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingTekstTilBrevService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRammebehandlingFraKlageService
import no.nav.tiltakspenger.saksbehandling.klage.service.VurderKlagebehandlingService
import java.time.Clock

fun Route.klagebehandlingRoutes(
    opprettKlagebehandlingService: OpprettKlagebehandlingService,
    oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService,
    avbrytKlagebehandlingService: AvbrytKlagebehandlingService,
    forhåndsvisBrevKlagebehandlingService: ForhåndsvisBrevKlagebehandlingService,
    oppdaterKlagebehandlingTekstTilBrevService: OppdaterKlagebehandlingTekstTilBrevService,
    iverksettKlagebehandlingService: IverksettKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    vurderKlagebehandlingService: VurderKlagebehandlingService,
    opprettRammebehandlingFraKlageService: OpprettRammebehandlingFraKlageService,
    clock: Clock,
) {
    opprettKlagebehandlingRoute(
        opprettKlagebehandlingService = opprettKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    oppdaterKlagebehandlingFormkravRoute(
        oppdaterKlagebehandlingFormkravService = oppdaterKlagebehandlingFormkravService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    avbrytKlagebehandlingRoute(
        avbrytKlagebehandlingService = avbrytKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    forhåndsvisBrevKlagebehandlingRoute(
        forhåndsvisBrevKlagebehandlingService = forhåndsvisBrevKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    oppdaterTekstTilBrev(
        oppdaterKlagebehandlingTekstTilBrevService = oppdaterKlagebehandlingTekstTilBrevService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    iverksettKlagebehandlingRoute(
        iverksettKlagebehandlingService = iverksettKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    vurderKlagebehandlingRoute(
        vurderKlagebehandlingService = vurderKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    opprettRammebehandlingFraKlage(
        opprettRammebehandlingFraKlageService = opprettRammebehandlingFraKlageService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
}
