package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeMigrereTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppdragsmigreringenStårIEnAnnenTilstand
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.MigrerTilOppdragsversjon3Service

private const val MIGRER_TIL_OPPDRAGSVERSJON_3_PATH = "$SAK_PATH/{sakId}/migrer-til-oppdragsversjon-3"

private data class MigrertTilOppdragsversjon3DTO(
    val sakId: String,
    val saksnummer: String,
    val oppdragsversjon: String,
    val alleredeMigrert: Boolean,
)

/**
 * Sperrer utbetalingene i saken og migrerer den; en jobb opphever sperren når Hel Ved har tatt opp de migrerte kjedene.
 * Åpen for alle saksbehandlerroller; tjenesten avviser kallet i prod.
 */
fun Route.migrerTilOppdragsversjon3Route(
    migrerTilOppdragsversjon3Service: MigrerTilOppdragsversjon3Service,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(MIGRER_TIL_OPPDRAGSVERSJON_3_PATH) {
        logger.debug { "Mottatt post-request på $MIGRER_TIL_OPPDRAGSVERSJON_3_PATH" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        call.withSakId { sakId ->
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
            auditService.logMedSakId(
                sakId = sakId,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.UPDATE,
                contextMessage = "Migrerer saken til oppdragsversjon 3 hos Hel Ved",
                correlationId = call.correlationId(),
                behandlingId = null,
            )
            val opphevSperrenStraks = call.request.queryParameters["opphevSperrenStraks"] == "true"
            migrerTilOppdragsversjon3Service.migrer(sakId, opphevSperrenStraks).fold(
                ifLeft = {
                    call.loggOgSvarFeil(logger, "Migrering til oppdragsversjon 3", it, it.tilStatusOgErrorJson(), "sakId: $sakId, correlationId: ${call.correlationId()}")
                },
                ifRight = {
                    call.respondJson(
                        value = MigrertTilOppdragsversjon3DTO(
                            sakId = it.sakId.toString(),
                            saksnummer = it.saksnummer.verdi,
                            oppdragsversjon = Oppdragsversjon.V3.name,
                            alleredeMigrert = it.alleredeMigrert,
                        ),
                    )
                },
            )
        }
    }
}

private fun KanIkkeMigrereTilOppdragsversjon3.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeMigrereTilOppdragsversjon3.FunksjonenErIkkePåslått -> HttpStatusCode.BadRequest to ErrorJson(
            melding = "Migrering til oppdragsversjon 3 er ikke påslått i dette miljøet",
            kode = "migrering_til_oppdragsversjon_3_er_ikke_påslått",
        )

        is KanIkkeMigrereTilOppdragsversjon3.HarUtbetalingSomIkkeErKvittert -> HttpStatusCode.Conflict to ErrorJson(
            melding = "Saken har utbetalinger som ikke er kvittert OK: $utbetalingIder",
            kode = "har_utbetaling_som_ikke_er_kvittert",
        )

        is KanIkkeMigrereTilOppdragsversjon3.HelvedHarIkkeKvittertSisteIverksetting -> HttpStatusCode.Conflict to ErrorJson(
            melding = "Hel Ved har ikke kvittert siste iverksetting på saken. Prøv igjen senere.",
            kode = "helved_har_ikke_kvittert_siste_iverksetting",
        )

        is KanIkkeMigrereTilOppdragsversjon3.HelvedFeilet -> HttpStatusCode.BadGateway to ErrorJson(
            melding = "Migreringen hos Hel Ved feilet. Saken står sperret på oppdragsversjon 2, og migreringen kan kjøres på nytt.",
            kode = "helved_migrering_feilet",
        )

        is OppdragsmigreringenStårIEnAnnenTilstand -> HttpStatusCode.Conflict to ErrorJson(
            melding = "Et annet kall endret migreringen samtidig, så saken kan hentes på nytt.",
            kode = "oppdragsmigreringen_står_i_en_annen_tilstand",
        )
    }
}
