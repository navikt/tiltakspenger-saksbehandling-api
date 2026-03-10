package no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withDokumentInfoId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.klage.service.KunneIkkeViseInnstillingsbrev
import no.nav.tiltakspenger.saksbehandling.klage.service.VisInnstillingsbrevKlagebehandlingCommand
import no.nav.tiltakspenger.saksbehandling.klage.service.VisInnstillingsbrevKlagebehandlingService

internal const val VIS_INNSTILLINGSBREV_KLAGEBEHANDLING_PATH =
    "/sak/{sakId}/klage/{klagebehandlingId}/innstillingsbrev/{dokumentInfoId}"

fun Route.visInnstillingsbrevKlagebehandlingRoute(
    visInnstillingsbrevService: VisInnstillingsbrevKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    get(VIS_INNSTILLINGSBREV_KLAGEBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på $VIS_INNSTILLINGSBREV_KLAGEBEHANDLING_PATH - saksbehandler ønsker å vise innstillingsbrev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withDokumentInfoId { dokumentInfoId ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    visInnstillingsbrevService.hentDokument(
                        VisInnstillingsbrevKlagebehandlingCommand(
                            sakId = sakId,
                            klagebehandlingId = klagebehandlingId,
                            dokumentInfoId = dokumentInfoId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                        },
                        ifRight = {
                            auditService.logMedSakId(
                                sakId = sakId,
                                behandlingId = klagebehandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Viser innstillingsbrev for klagebehandling",
                                correlationId = correlationId,
                            )
                            call.respondBytes(it.getContent(), ContentType.Application.Pdf)
                        },
                    )
                }
            }
        }
    }
}

fun KunneIkkeViseInnstillingsbrev.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KunneIkkeViseInnstillingsbrev.KlagenErIkkeJournalført -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Klagen er ikke journalført, og det finnes derfor ikke noe innstillingsbrev å vise",
        kode = "klage_ikke_journalført",
    )
}
