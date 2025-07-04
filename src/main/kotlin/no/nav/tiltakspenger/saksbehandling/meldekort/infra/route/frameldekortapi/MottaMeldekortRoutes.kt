package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.frameldekortapi

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.krevLagreMeldekortRollen
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeLagreBrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService

private const val PATH = "/meldekort/motta"

internal fun Route.mottaMeldekortRoutes(
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH" }
        call.withSystembruker(tokenService = tokenService) { systembruker: Systembruker ->
            call.withBody<BrukerutfyltMeldekortDTO> { meldekort ->
                logger.info { "Mottatt meldekort fra bruker: ${meldekort.id} - meldeperiode: ${meldekort.meldeperiodeId}" }

                krevLagreMeldekortRollen(systembruker)
                mottaBrukerutfyltMeldekortService.mottaBrukerutfyltMeldekort(meldekort.toDomain()).onRight {
                    call.respond(HttpStatusCode.OK)
                }.onLeft {
                    when (it) {
                        is KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff -> call.respond(
                            status = HttpStatusCode.OK,
                            message = "Meldekort med id ${meldekort.id} var allerede lagret med samme data",
                        )

                        is KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff -> call.respond(
                            status = HttpStatusCode.Conflict,
                            message = "Meldekort med id ${meldekort.id} var allerede lagret med andre data!",
                        )

                        is KanIkkeLagreBrukersMeldekort.UkjentFeil -> call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = "Kunne ikke lagre brukers meldekort - Ukjent feil",
                        )
                    }
                }
            }
        }
    }
}

private fun BrukerutfyltMeldekortDTO.toDomain(): LagreBrukersMeldekortKommando {
    return LagreBrukersMeldekortKommando(
        id = MeldekortId.fromString(this.id),
        mottatt = this.mottatt,
        meldeperiodeId = MeldeperiodeId.fromString(this.meldeperiodeId),
        sakId = SakId.fromString(this.sakId),
        dager = this.dager.map {
            BrukersMeldekortDag(
                dato = it.key,
                status = when (it.value) {
                    BrukerutfyltMeldekortDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET -> InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                    BrukerutfyltMeldekortDTO.Status.DELTATT_MED_LØNN_I_TILTAKET -> InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_SYK -> InnmeldtStatus.FRAVÆR_SYK
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_SYKT_BARN -> InnmeldtStatus.FRAVÆR_SYKT_BARN
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_GODKJENT_AV_NAV -> InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV
                    BrukerutfyltMeldekortDTO.Status.FRAVÆR_ANNET -> InnmeldtStatus.FRAVÆR_ANNET
                    BrukerutfyltMeldekortDTO.Status.IKKE_BESVART -> InnmeldtStatus.IKKE_BESVART
                },
            )
        },
        journalpostId = JournalpostId(this.journalpostId),
    )
}
