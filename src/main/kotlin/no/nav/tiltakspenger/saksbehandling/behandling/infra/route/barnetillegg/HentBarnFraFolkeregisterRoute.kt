package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteBarnFraFolkeregister
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.person.infra.route.FnrDTO
import no.nav.tiltakspenger.saksbehandling.person.infra.route.sladdetFor
import no.nav.tiltakspenger.saksbehandling.person.infra.route.toEnkelPersonDTO

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/barn/folkeregister"

/** Slår opp et barn saksbehandler vil legge til i barnetillegget, og verifiserer at det er søkers barn. */
fun Route.hentBarnFraFolkeregisterRoute(
    sakService: SakService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - slår opp barn i folkeregisteret" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<FnrDTO> { body ->
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    val barnFnr = Fnr.tryFromString(body.fnr.trim()) ?: return@withBody call.respond400BadRequest(
                        melding = "Fødselsnummeret må være 11 siffer",
                        kode = "ugyldig_fnr",
                    )
                    val correlationId = call.correlationId()

                    sakService.hentBarnFraFolkeregister(sakId, barnFnr, correlationId).fold(
                        ifLeft = {
                            when (it) {
                                KunneIkkeHenteBarnFraFolkeregister.IkkeBarnAvSøker -> call.respond404NotFound(
                                    melding = "Fant ikke barnet blant søkers barn i folkeregisteret",
                                    kode = "barn_ikke_funnet",
                                )

                                is KunneIkkeHenteBarnFraFolkeregister.FeilVedOppslag -> when (it.feil) {
                                    KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl -> call.respond500InternalServerError(
                                        melding = "Feil ved kall mot PDL",
                                        kode = "feil_ved_kall_mot_pdl",
                                    )

                                    KunneIkkeHenteEnkelPerson.FeilVedKallMotSkjerming -> call.respond500InternalServerError(
                                        melding = "Feil ved kall mot skjermingstjenesten",
                                        kode = "feil_ved_kall_mot_skjerming",
                                    )
                                }
                            }
                        },
                        ifRight = { barn ->
                            // Personen utledes fra sakId, som er det oppslaget og tilgangssjekken faktisk gjelder.
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Slår opp barn i folkeregisteret for barnetillegg",
                                correlationId = correlationId,
                                behandlingId = behandlingId,
                            )
                            call.respondJson(value = barn.toEnkelPersonDTO().sladdetFor(saksbehandler))
                        },
                    )
                }
            }
        }
    }
}
