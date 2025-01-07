package no.nav.tiltakspenger.vedtak.routes.meldekort

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.fantIkkeMeldekort
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond404NotFound
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.withMeldekortId
import no.nav.tiltakspenger.vedtak.routes.withSakId

fun Route.hentMeldekortRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    get("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Motatt get-request på /sak/{sakId}/meldekort/{meldekortId}" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    val correlationId = call.correlationId()

                    val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId = correlationId).getOrElse {
                        when (it) {
                            is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(ikkeTilgang("Må ha en av rollene ${it.kreverEnAvRollene} for å hente meldekort"))
                        }
                        return@withMeldekortId
                    }
                    val meldekort = sak.hentMeldekort(meldekortId)

                    if (meldekort == null) {
                        call.respond404NotFound(fantIkkeMeldekort())
                        return@withMeldekortId
                    }
                    if (meldekort is MeldekortBehandling.IkkeUtfyltMeldekort && !meldekort.erKlarTilUtfylling()) {
                        call.respond400BadRequest(
                            melding = "Meldekortet er ikke klart til utfylling",
                            kode = "meldekortet_er_ikke_klart_til_utfylling",
                        )
                        return@withMeldekortId
                    }
                    val forrigeMeldekort: MeldekortBehandling.UtfyltMeldekort? =
                        meldekort.forrigeMeldekortId?.let { sak.hentMeldekort(it) as MeldekortBehandling.UtfyltMeldekort }
                    val forrigeNavkontor = forrigeMeldekort?.navkontor

                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter meldekort",
                        correlationId = correlationId,
                    )
                    // TODO post-mvp jah: Saksbehandlerne reagerte på ordet saksperiode og ønsket seg "vedtaksperiode". Gitt at man har en forlengelse vil man har et førstegangsvedtak+forlengelsesvedtak. Vil de ikke ha se den totale meldeperioden for den gitte saken?
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = meldekort.toDTO(
                            vedtaksPeriode = sak.vedtaksperiode!!,
                            tiltaksnavn = sak.hentTiltaksnavn()!!,
                            antallDager = sak.hentAntallDager()!!,
                            forrigeNavkontor = forrigeNavkontor,
                        ),
                    )
                }
            }
        }
    }
}
