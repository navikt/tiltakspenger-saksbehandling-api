package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.auth2.TokenService
import no.nav.tiltakspenger.vedtak.routes.Standardfeil.fantIkkeMeldekort
import no.nav.tiltakspenger.vedtak.routes.Standardfeil.fantIkkeSak
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.respond404NotFound
import no.nav.tiltakspenger.vedtak.routes.withMeldekortId
import no.nav.tiltakspenger.vedtak.routes.withSakId
import no.nav.tiltakspenger.vedtak.routes.withSaksbehandler

fun Route.hentMeldekortRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    get("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Motatt get-request på /sak/{sakId}/meldekort/{meldekortId}" }
        call.withSaksbehandler(tokenService = tokenService) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    val correlationId = call.correlationId()

                    val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId = correlationId)
                    if (sak == null) {
                        call.respond404NotFound(fantIkkeSak())
                        return@withMeldekortId
                    }

                    val meldekort = sak.hentMeldekort(meldekortId)

                    if (meldekort == null) {
                        call.respond404NotFound(fantIkkeMeldekort())
                        return@withMeldekortId
                    }
                    if (meldekort is Meldekort.IkkeUtfyltMeldekort && !meldekort.erKlarTilUtfylling()) {
                        call.respond400BadRequest(
                            melding = "Meldekortet er ikke klart til utfylling",
                            kode = "meldekortet_er_ikke_klart_til_utfylling",
                        )
                        return@withMeldekortId
                    }
                    val forrigeMeldekort: Meldekort.UtfyltMeldekort? =
                        meldekort.forrigeMeldekortId?.let { sak.hentMeldekort(it) as Meldekort.UtfyltMeldekort }
                    val forrigeNavkontor = forrigeMeldekort?.navkontor

                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter meldekort",
                        correlationId = correlationId,
                    )
                    // TODO pre-mvp: Her blir det mer riktig og bruke den totale perioden det skal meldes for.
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = meldekort.toDTO(
                            sak.vedtaksperiode!!,
                            sak.hentRelatertTiltak()!!,
                            sak.hentAntallDager()!!,
                            forrigeNavkontor,
                        ),
                    )
                }
            }
        }
    }
}
