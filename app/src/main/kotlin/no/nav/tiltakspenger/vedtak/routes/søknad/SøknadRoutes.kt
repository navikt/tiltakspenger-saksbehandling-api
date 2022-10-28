package no.nav.tiltakspenger.vedtak.routes.søknad

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.SøknadId
import no.nav.tiltakspenger.vedtak.audit.auditHvisInnlogget
import no.nav.tiltakspenger.vedtak.service.søknad.StorSøknadDTO
import no.nav.tiltakspenger.vedtak.service.søknad.SøknadService
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetBrukerProvider

private val LOG = KotlinLogging.logger {}

internal const val personsøknadPath = "/person/søknad"
internal const val søknadPath = "/søknad"

data class SøknadBody(
    val ident: String
)

fun Route.søknadRoutes(
    innloggetBrukerProvider: InnloggetBrukerProvider,
    søknadService: SøknadService,
) {
    get("${søknadPath}/{søknadId}") {
        val søknadId = call.parameters["søknadId"]
            ?: return@get call.respond(message = "Mangler soknadId", status = HttpStatusCode.NotFound)
        LOG.info { "Vi har truffet GET /søknad" }

        val behandlingAvSøknad = søknadService.hentBehandlingAvSøknad(søknadId)
            ?: return@get call.respond(message = "Søknad ikke funnet", status = HttpStatusCode.NotFound)

        call.auditHvisInnlogget(berørtBruker = behandlingAvSøknad.personopplysninger.ident)
        call.respond(message = behandlingAvSøknad, status = HttpStatusCode.OK)
    }

    post("$personsøknadPath/{søknadId}") {
        val søknadId: SøknadId = call.parameters["søknadId"]?.let {
            SøknadId.fromDb(it)
        } ?: return@post call.respond(message = "Må oppgi en SøknadId", status = HttpStatusCode.NotFound)

        val ident = call.receive<SøknadBody>().ident

        call.auditHvisInnlogget(berørtBruker = ident)

        val response: StorSøknadDTO? = søknadService.hentSøknad(ident, søknadId)
        if (response == null) {
            call.respond(message = "Søknad ikke funnet", status = HttpStatusCode.NotFound)
        } else {
            call.respond(message = response, status = HttpStatusCode.OK)
        }

    }
}
