package no.nav.tiltakspenger.vedtak.routes.meldekort.frameldekortapi

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.meldekort.domene.NyttBrukersMeldekort
import no.nav.tiltakspenger.meldekort.service.MottaBrukerutfyltMeldekortService

private const val PATH = "/meldekort/motta"

fun Route.mottaMeldekortRoutes(
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH" }
        call.withBody<BrukerutfyltMeldekortDTO> { meldekort ->
            logger.info { "Mottatt meldekort fra bruker: ${meldekort.id} - meldeperiode: ${meldekort.meldeperiodeId}" }

            Either.catch {
                mottaBrukerutfyltMeldekortService.mottaBrukerutfyltMeldekort(meldekort.toDomain())
                call.respond(HttpStatusCode.OK)
            }.mapLeft { ex ->
                with("Kunne ikke lagre brukers meldekort ${meldekort.id}") {
                    logger.error { this }
                    sikkerlogg.error(ex) { "$this - ${ex.message}" }
                }

                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = "Kunne ikke lagre brukers meldekort",
                )
            }
        }
    }
}

private fun BrukerutfyltMeldekortDTO.toDomain(): NyttBrukersMeldekort {
    return NyttBrukersMeldekort(
        id = MeldekortId.fromString(this.id),
        mottatt = this.mottatt,
        meldeperiodeId = MeldeperiodeId.fromString(this.meldeperiodeId),
        sakId = SakId.fromString(this.sakId),
        dager = this.dager.map {
            BrukersMeldekortDag(
                dato = it.key,
                status = when (it.value) {
                    BrukerutfyltMeldekortDTO.Status.DELTATT -> InnmeldtStatus.DELTATT
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_SYK -> InnmeldtStatus.FRAVÆR_SYK
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_SYKT_BARN -> InnmeldtStatus.FRAVÆR_SYKT_BARN
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_ANNET -> InnmeldtStatus.FRAVÆR_ANNET
                    BrukerutfyltMeldekortDTO.Status.IKKE_DELTATT -> InnmeldtStatus.IKKE_DELTATT
                    BrukerutfyltMeldekortDTO.Status.IKKE_REGISTRERT -> InnmeldtStatus.IKKE_REGISTRERT
                    BrukerutfyltMeldekortDTO.Status.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER
                },
            )
        },
        journalpostId = JournalpostId(this.journalpostId),
        oppgaveId = null,
    )
}
