package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.kanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KanIkkeVurdereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak.ANNET
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak.FEIL_LOVANVENDELSE
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak.PROSESSUELL_FEIL
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.OmgjørKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.VurderKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

private data class VurderKlagebehandlingBody(
    val begrunnelse: String,
    val årsak: String,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        klagebehandlingId: KlagebehandlingId,
    ): VurderKlagebehandlingKommando {
        return OmgjørKlagebehandlingKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            begrunnelse = Begrunnelse.create(begrunnelse)!!,
            årsak = when (årsak) {
                "FEIL_LOVANVENDELSE" -> FEIL_LOVANVENDELSE
                "FEIL_REGELVERKSFORSTAAELSE" -> FEIL_REGELVERKSFORSTAAELSE
                "FEIL_ELLER_ENDRET_FAKTA" -> FEIL_ELLER_ENDRET_FAKTA
                "PROSESSUELL_FEIL" -> PROSESSUELL_FEIL
                "ANNET" -> ANNET
                else -> throw IllegalArgumentException("Ukjent omgjøringsårsak: $årsak")
            },
        )
    }
}

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/vurder"

fun Route.vurderKlagebehandlingRoute(
    vurderKlagebehandlingService: VurderKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Vurderer klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<VurderKlagebehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    vurderKlagebehandlingService.vurder(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            klagebehandlingId = klagebehandlingId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(it.toStatusAndErrorJson())
                        },
                        ifRight = { (_, behandling) ->
                            val behandlingId = behandling.id
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Vurderer klagebehandling på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = behandlingId,
                            )
                            call.respondJson(value = behandling.toDto())
                        },
                    )
                }
            }
        }
    }
}

fun KanIkkeVurdereKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeVurdereKlagebehandling.SaksbehandlerMismatch -> {
            Pair(
                HttpStatusCode.BadRequest,
                behandlingenEiesAvAnnenSaksbehandler(
                    this.forventetSaksbehandler,
                ),
            )
        }

        is KanIkkeVurdereKlagebehandling.KanIkkeOppdateres -> {
            Pair(
                HttpStatusCode.BadRequest,
                kanIkkeOppdatereBehandling(),
            )
        }
    }
}
