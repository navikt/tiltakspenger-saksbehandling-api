package no.nav.tiltakspenger.saksbehandling.routes.sak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.routes.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.KunneIkkeAvbryteSøknadOgBehandling
import java.time.Clock

fun Route.avbrytSøknadOgBehandling(
    tokenService: TokenService,
    auditService: AuditService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post("$SAK_PATH/{saksnummer}/avbryt-aktiv-behandling") {
        logger.debug { "Mottatt post-request på $SAK_PATH/{saksnummer}/avbryt-aktiv-behandling - Prøver å avslutte søknad og behandling" }
        call.withSaksbehandler(tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSaksnummer { saksnummer ->
                call.withBody<AvsluttSøknadOgBehandlingBody> { body ->
                    avbrytSøknadOgBehandlingService.avbrytSøknadOgBehandling(
                        body.toCommand(
                            saksnummer = saksnummer,
                            avsluttetAv = saksbehandler,
                            correlationId = call.correlationId(),
                        ),
                    ).fold(
                        {
                            val (status, message) = it.toStatusAndMessage()
                            call.respond(status, message)
                        },
                        {
                            auditService.logMedSaksnummer(
                                saksnummer = saksnummer,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                correlationId = call.correlationId(),
                                contextMessage = "Avsluttet søknad og behandling",
                            )
                            call.respond(status = HttpStatusCode.OK, it.toSakDTO(clock))
                        },
                    )
                }
            }
        }
    }
}

fun KunneIkkeAvbryteSøknadOgBehandling.toStatusAndMessage(): Pair<HttpStatusCode, String> = when (this) {
    KunneIkkeAvbryteSøknadOgBehandling.Feil -> HttpStatusCode.InternalServerError to "Ukjent feil ved avbrytelse av søknad (og behandling)"
}

data class AvsluttSøknadOgBehandlingBody(
    val søknadId: String?,
    val behandlingId: String?,
    val begrunnelse: String,
) {
    fun toCommand(
        saksnummer: Saksnummer,
        avsluttetAv: Saksbehandler,
        correlationId: CorrelationId,
    ): AvbrytSøknadOgBehandlingCommand {
        return AvbrytSøknadOgBehandlingCommand(
            saksnummer = saksnummer,
            søknadId = søknadId?.let { SøknadId.fromString(it) },
            behandlingId = behandlingId?.let { BehandlingId.fromString(it) },
            avsluttetAv = avsluttetAv,
            correlationId = correlationId,
            begrunnelse = begrunnelse,

        )
    }
}
