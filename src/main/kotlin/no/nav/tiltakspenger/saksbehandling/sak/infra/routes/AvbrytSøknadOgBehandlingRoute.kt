package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.KunneIkkeAvbryteSøknadOgBehandling
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock

fun Route.avbrytSøknadOgBehandling(
    auditService: AuditService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("$SAK_PATH/{saksnummer}/avbryt-aktiv-behandling") {
        logger.debug { "Mottatt post-request på $SAK_PATH/{saksnummer}/avbryt-aktiv-behandling - Prøver å avslutte søknad og behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSaksnummer { saksnummer ->
            call.withBody<AvsluttSøknadOgBehandlingBody> { body ->
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, token)
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
            begrunnelse = saniter(begrunnelse),

        )
    }
}
