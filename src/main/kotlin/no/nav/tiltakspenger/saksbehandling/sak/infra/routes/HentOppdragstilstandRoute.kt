package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo

private const val HENT_OPPDRAGSTILSTAND_PATH = "$SAK_PATH/{saksnummer}/oppdragstilstand"

private data class OppdragstilstandDTO(
    val sakId: String,
    val saksnummer: String,
    val oppdragsversjon: String,
    val oppdragsmigrering: String?,
    val sperrerUtbetalinger: Boolean,
    val migrertHosHelVedTidspunkt: String?,
)

fun Route.hentOppdragstilstandRoute(
    sakService: SakService,
    utbetalingRepo: UtbetalingRepo,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    get(HENT_OPPDRAGSTILSTAND_PATH) {
        logger.debug { "Mottatt get-request på $HENT_OPPDRAGSTILSTAND_PATH" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get

        call.withSaksnummer { saksnummer ->
            val sak = try {
                sakService.hentForSaksnummer(saksnummer)
            } catch (_: IkkeFunnetException) {
                logger.warn { "Fant ikke sak med saksnummer $saksnummer" }
                call.respond(HttpStatusCode.NotFound, "Fant ikke sak med saksnummer $saksnummer")
                return@withSaksnummer
            }

            tilgangskontrollService.harTilgangTilPerson(sak.fnr, token, saksbehandler)
            auditService.logMedSaksnummer(
                saksnummer = saksnummer,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.ACCESS,
                contextMessage = "Henter oppdragstilstand for saken",
                correlationId = call.correlationId(),
            )
            val oppdragstilstand = utbetalingRepo.hentOppdragstilstand(sak.id)
            call.respondJson(
                value = OppdragstilstandDTO(
                    sakId = sak.id.toString(),
                    saksnummer = sak.saksnummer.verdi,
                    oppdragsversjon = oppdragstilstand.versjon.name,
                    oppdragsmigrering = oppdragstilstand.migrering?.name,
                    sperrerUtbetalinger = oppdragstilstand.sperrerUtbetalinger,
                    migrertHosHelVedTidspunkt = oppdragstilstand.migrertHosHelVedTidspunkt?.toString(),
                ),
            )
        }
    }
}
