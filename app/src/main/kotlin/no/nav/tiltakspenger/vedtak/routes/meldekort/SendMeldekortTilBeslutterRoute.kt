package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Dag
import no.nav.tiltakspenger.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider
import java.time.LocalDate

private data class Body(
    val dager: List<Dag>,
) {
    data class Dag(
        val dato: String,
        val status: String,
    )

    fun toDomain(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        sakId: SakId,
    ) = SendMeldekortTilBeslutterKommando(
        sakId = sakId,
        saksbehandler = saksbehandler,
        dager =
        this.dager.map { dag ->
            Dag(
                dag = LocalDate.parse(dag.dato),
                status =
                when (dag.status) {
                    "SPERRET" -> SendMeldekortTilBeslutterKommando.Status.SPERRET
                    "DELTATT_UTEN_LØNN_I_TILTAKET" -> SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                    "DELTATT_MED_LØNN_I_TILTAKET" -> SendMeldekortTilBeslutterKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                    "IKKE_DELTATT" -> SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
                    "FRAVÆR_SYK" -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYK
                    "FRAVÆR_SYKT_BARN" -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYKT_BARN
                    "FRAVÆR_VELFERD_GODKJENT_AV_NAV" -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                    "FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV" -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                    else -> throw IllegalArgumentException("Ukjent status: ${dag.status}")
                },
            )
        },
        meldekortId = meldekortId,
    )
}

fun Route.sendMeldekortTilBeslutterRoute(
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    innloggetSaksbehandlerProvider: InnloggetSaksbehandlerProvider,
) {
    post("/sak/{sakId}/meldekort/{meldekortId}") {
        val saksbehandler: Saksbehandler = innloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(call)
        val meldekortId =
            call.parameters["meldekortId"]
                ?: return@post call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)
        val sakId = call.parameters["sakId"]
            ?: return@post call.respond(message = "sakId mangler", status = HttpStatusCode.NotFound)
        val dto = call.receive<Body>()
        val meldekort =
            sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                dto.toDomain(
                    saksbehandler = saksbehandler,
                    meldekortId = MeldekortId.fromString(meldekortId),
                    sakId = SakId.fromString(sakId),
                ),
            )
        meldekort.fold(
            ifLeft = {
                call.respond(
                    message = when (it) {
                        KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid -> "Kan ikke sende inn et meldekort før meldekortperioden har begynt."
                    },
                    status = HttpStatusCode.BadRequest,
                )
            },
            ifRight = {
                call.respond(message = it.toDTO(), status = HttpStatusCode.OK)
            },
        )
    }
}
