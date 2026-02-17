package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt.avbrytKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.forhåndsvisBrevKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.oppdaterTekstTilBrev
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.oppdaterKlagebehandlingFormkravRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta.gjenopptaKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.iverksettAvvistKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake.leggTilbakeKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprett.opprettKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettRammebehandling.opprettRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold.opprettholdKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta.overtaKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent.settKlagebehandlingPåVentRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.taKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.vurderKlagebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.klage.service.AvbrytKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.ForhåndsvisBrevKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.GjenopptaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.IverksettAvvistKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.LeggTilbakeKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingTekstTilBrevService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRammebehandlingFraKlageService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettholdKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OvertaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.SettKlagebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.klage.service.TaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.VurderKlagebehandlingService
import java.time.Clock

fun Route.klagebehandlingRoutes(
    opprettKlagebehandlingService: OpprettKlagebehandlingService,
    oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService,
    avbrytKlagebehandlingService: AvbrytKlagebehandlingService,
    forhåndsvisBrevKlagebehandlingService: ForhåndsvisBrevKlagebehandlingService,
    oppdaterKlagebehandlingTekstTilBrevService: OppdaterKlagebehandlingTekstTilBrevService,
    iverksettAvvistKlagebehandlingService: IverksettAvvistKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    vurderKlagebehandlingService: VurderKlagebehandlingService,
    opprettRammebehandlingFraKlageService: OpprettRammebehandlingFraKlageService,
    overtaKlagebehandlingService: OvertaKlagebehandlingService,
    taKlagebehandlingService: TaKlagebehandlingService,
    leggTilbakeKlagebehandlingService: LeggTilbakeKlagebehandlingService,
    gjenopptaKlagebehandlingService: GjenopptaKlagebehandlingService,
    settKlagebehandlingPåVentService: SettKlagebehandlingPåVentService,
    opprettholdKlagebehandlingService: OpprettholdKlagebehandlingService,
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
    iverksettAvvistKlagebehandlingRoute(
        iverksettAvvistKlagebehandlingService = iverksettAvvistKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
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
    overtaKlagebehandlingRoute(
        overtaKlagebehandlingService = overtaKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    taKlagebehandlingRoute(
        taKlagebehandlingService = taKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    leggTilbakeKlagebehandlingRoute(
        leggTilbakeKlagebehandlingService = leggTilbakeKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    settKlagebehandlingPåVentRoute(
        settKlagebehandlingPåVentService = settKlagebehandlingPåVentService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    gjenopptaKlagebehandlingRoute(
        gjenopptaKlagebehandlingService = gjenopptaKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    opprettholdKlagebehandlingRoute(
        opprettholdKlagebehandlingService = opprettholdKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
}
