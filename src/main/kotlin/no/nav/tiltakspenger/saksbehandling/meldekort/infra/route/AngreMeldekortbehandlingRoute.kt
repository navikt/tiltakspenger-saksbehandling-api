package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.KanIkkeAngreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AngreMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/angre"

fun Route.angreMeldekortbehandlingRoute(
    auditService: AuditService,
    angreMeldekortbehandlingService: AngreMeldekortbehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Saksbehandler forsøker å angre meldekortbehandling sendt til beslutning" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()

                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, saksbehandlerToken = token)

                angreMeldekortbehandlingService.angreMeldekortbehandling(
                    sakId,
                    meldekortId,
                    saksbehandler,
                ).fold(
                    ifLeft = { feil ->
                        call.loggOgSvarFeil(
                            logger,
                            operasjon = "Angre meldekortbehandling",
                            feil = feil,
                            statusOgErrorJson = feil.statusOgErrorJson(),
                            kontekst = "sakId=$sakId, meldekortId=$meldekortId",
                        )
                    },
                    ifRight = { riktig ->
                        auditService.logMedMeldekortId(
                            meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler angrer meldekortbehandling",
                            correlationId = correlationId,
                        )
                        call.respondJson(value = riktig.first.toSakDTO(saksbehandler, clock))
                    },
                )
            }
        }
    }
}

fun KanIkkeAngreMeldekortbehandling.statusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeAngreMeldekortbehandling.KanIkkeVæreTattAvEnBeslutter -> HttpStatusCode.Forbidden to ErrorJson(
        "Kan ikke angre meldekortbehandling som er tatt av en beslutter",
        "meldekortbehandlingen_kan_ikke_være_tatt_av_beslutter",
    )

    KanIkkeAngreMeldekortbehandling.MeldekortbehandlingFinnesIkke -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke angre en meldekortbehandling som ikke finnes",
        "meldekortbehandlingen_må_eksistere",
    )

    is KanIkkeAngreMeldekortbehandling.MeldekortbehandlingenErIEnTilstandSomIkkeTillaterÅAngre -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke angre en meldekortbehandling som ikke er klar til beslutning, status er $status",
        "meldekortbehandlingen_må_være_klar_til_beslutning",
    )

    KanIkkeAngreMeldekortbehandling.MåVæreSammeSaksbehandlerForÅAngreMeldekortbehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandleren som er tildelt meldekortbehandling for å angre.",
        "maa_vaere_saksbehandler_for_meldekortbehandlingen",
    )
}
