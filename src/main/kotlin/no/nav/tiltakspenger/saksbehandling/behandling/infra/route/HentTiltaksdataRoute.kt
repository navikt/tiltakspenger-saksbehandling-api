package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentTiltaksdataService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.KunneIkkeHenteTiltaksdata
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.sladdetFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.toDTO

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/tiltaksdata"

/**
 * Henter opplysninger om tiltakene i behandlingens saksopplysninger som vi ikke lagrer, foreløpig kun tittelen.
 * Opplysningene slås opp hos kilden ved hvert kall.
 */
fun Route.hentTiltaksdataRoute(
    hentTiltaksdataService: HentTiltaksdataService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    get(PATH) {
        logger.debug { "Mottatt get-request på '$PATH' - henter tiltaksdata for behandlingen" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                hentTiltaksdataService.hent(sakId, behandlingId, correlationId).fold(
                    ifLeft = {
                        when (it) {
                            KunneIkkeHenteTiltaksdata.FeilVedKallMotPdl -> call.respond500InternalServerError(
                                melding = "Feil ved kall mot PDL",
                                kode = "feil_ved_kall_mot_pdl",
                            )

                            KunneIkkeHenteTiltaksdata.FeilVedKallMotTiltak -> call.respond500InternalServerError(
                                melding = "Feil ved henting av tiltaksdeltakelser",
                                kode = "feil_ved_kall_mot_tiltak",
                            )
                        }
                    },
                    ifRight = { tiltaksdata ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter tiltaksdata for en behandling",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = tiltaksdata.map { it.toDTO() }.sladdetFor(saksbehandler))
                    },
                )
            }
        }
    }
}
