package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldeperiode/{kjedeId}"

fun Route.hentMeldekortRoute(
    sakService: SakService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    get(PATH) {
        logger.debug { "Mottatt get-request på $PATH" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            call.withMeldeperiodeKjedeId { kjedeId ->
                val correlationId = call.correlationId()
                val sak = sakService.hentForSakId(sakId)
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPerson(sak.fnr, token, saksbehandler)

                val meldeperiodeKjedeDTO = sak.toMeldeperiodeKjedeDTO(kjedeId = kjedeId, clock = clock)

                auditService.logMedSakId(
                    sakId = sakId,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.ACCESS,
                    contextMessage = "Henter meldekort",
                    correlationId = correlationId,
                )
                call.respondJson(
                    value = meldeperiodeKjedeDTO,
                )
            }
        }
    }
}
