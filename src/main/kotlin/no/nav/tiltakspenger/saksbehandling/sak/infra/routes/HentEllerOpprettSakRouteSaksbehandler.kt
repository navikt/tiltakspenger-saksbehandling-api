package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.fantIkkeFnr
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon

fun Route.hentEllerOpprettSakRoute(
    sakService: SakService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    personService: PersonService,
    erProd: Boolean,
) {
    val logger = KotlinLogging.logger {}

    put(SAK_PATH) {
        logger.debug { "Mottatt kall på '$SAK_PATH' - henter eller oppretter sak." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@put
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@put
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        val correlationId = call.correlationId()
        call.withBody<HentEllerOpprettSakBody> { body ->
            val fnr: Fnr = Fnr.fromString(body.fnr)
            if (body.oppdragsversjon != null && erProd) {
                call.respond400BadRequest(
                    melding = "Oppdragsversjon kan bare velges utenfor prod.",
                    kode = "oppdragsversjon_kan_ikke_velges",
                )
                return@withBody
            }
            tilgangskontrollService.harTilgangTilPerson(
                fnr = fnr,
                saksbehandlerToken = token,
                saksbehandler = saksbehandler,
            )
            personService.hentEnkelPersonFnr(fnr).fold(
                ifLeft = {
                    call.respond404NotFound(fantIkkeFnr())
                },
                ifRight = {
                    val (sak, opprettet) = sakService.hentEllerOpprettSak(
                        fnr = fnr,
                        correlationId = correlationId,
                        oppdragsversjon = body.oppdragsversjon,
                    )
                    auditService.logMedSakId(
                        sakId = sak.id,
                        navIdent = saksbehandler.navIdent,
                        action = if (opprettet) AuditLogEvent.Action.CREATE else AuditLogEvent.Action.ACCESS,
                        contextMessage = "Hentet eller opprettet sak.",
                        correlationId = correlationId,
                        behandlingId = null,
                    )

                    call.respondJson(
                        value = HentEllerOpprettSakResponse(saksnummer = sak.saksnummer.verdi, opprettet = opprettet),
                    )
                },
            )
        }
    }
}

/**
 * @param oppdragsversjon lar testere velge versjon av Hel Veds grensesnitt når saken opprettes, og avvises i prod.
 */
data class HentEllerOpprettSakBody(
    val fnr: String,
    val oppdragsversjon: Oppdragsversjon? = null,
)

data class HentEllerOpprettSakResponse(
    val saksnummer: String,
    val opprettet: Boolean,
)
