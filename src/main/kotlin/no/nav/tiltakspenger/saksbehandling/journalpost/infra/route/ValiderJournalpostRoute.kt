package no.nav.tiltakspenger.saksbehandling.journalpost.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService

fun Route.validerJournalpostRoute(
    validerJournalpostService: ValiderJournalpostService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("/journalpost/valider") {
        logger.debug { "Mottatt post-request på '/journalpost/valider' - Sjekker at journalpost finnes og tilhører bruker." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withBody<ValiderJournalpostBody> { body ->
            krevSaksbehandlerRolle(saksbehandler)
            val fnr = Fnr.fromString(body.fnr)
            val journalpostId = JournalpostId(body.journalpostId)
            tilgangskontrollService.harTilgangTilPerson(fnr, token, saksbehandler)
            val response = validerJournalpostService.hentOgValiderJournalpost(fnr, journalpostId)

            call.respond(
                HttpStatusCode.OK,
                response,
            )
        }
    }
}

data class ValiderJournalpostBody(
    val fnr: String,
    val journalpostId: String,
)

data class ValiderJournalpostResponse(
    val journalpostFinnes: Boolean,
    val gjelderInnsendtFnr: Boolean?,
)
