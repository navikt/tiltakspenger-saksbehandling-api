package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.ForMangeDagerUtfylt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.KanIkkeEndreDagFraSperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.KanIkkeEndreDagTilSperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.KunneIkkeHenteSak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.MåVæreSaksbehandler
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import java.time.Clock

fun Route.sendMeldekortTilBeslutterRoute(
    oppdaterMeldekortService: OppdaterMeldekortService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Mottatt post-request på /sak/{sakId}/meldekort/{meldekortId} - saksbehandler har fylt ut meldekortet og sendt til beslutter" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    call.withBody<OppdaterMeldekortDTO> { body ->
                        val correlationId = call.correlationId()
                        val kommando = body.toDomain(
                            saksbehandler = saksbehandler,
                            meldekortId = meldekortId,
                            sakId = sakId,
                            correlationId = correlationId,
                        )
                        oppdaterMeldekortService.sendMeldekortTilBeslutter(kommando).fold(
                            ifLeft = {
                                when (it) {
                                    is MeldekortperiodenKanIkkeVæreFremITid -> {
                                        call.respond400BadRequest(
                                            melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                                            kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
                                        )
                                    }

                                    is MåVæreSaksbehandler -> {
                                        call.respond400BadRequest(
                                            melding = "Kan ikke sende meldekort til beslutter. Krever saksbehandler-rolle.",
                                            kode = "må_være_saksbehandler",
                                        )
                                    }

                                    is ForMangeDagerUtfylt -> {
                                        call.respond400BadRequest(
                                            melding = "Kan ikke sende meldekort til beslutter. For mange dager er utfylt. Maks antall for dette meldekortet er ${it.maksDagerMedTiltakspengerForPeriode}, mens antall utfylte dager er ${it.antallDagerUtfylt}.",
                                            kode = "for_mange_dager_utfylt",
                                        )
                                    }

                                    is KunneIkkeHenteSak -> when (
                                        val u =
                                            it.underliggende
                                    ) {
                                        is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(
                                            Standardfeil.ikkeTilgang("Må ha en av rollene ${u.kreverEnAvRollene} for å hente sak"),
                                        )
                                    }

                                    KanIkkeEndreDagTilSperret, KanIkkeEndreDagFraSperret -> call.respond400BadRequest(
                                        melding = "Kan ikke endre dager som er sperret.",
                                        kode = "kan_ikke_endre_dager_som_er_sperret",
                                    )

                                    KanIkkeOppdatereMeldekort.InnsendteDagerMåMatcheMeldeperiode -> call.respond400BadRequest(
                                        melding = "Innsendte dager må matche meldeperiode.",
                                        kode = "innsendte_dager_må_matche_meldeperiode",
                                    )
                                }
                            },
                            ifRight = {
                                auditService.logMedMeldekortId(
                                    meldekortId = meldekortId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Saksbehandler har fylt ut meldekortet og sendt til beslutter",
                                    correlationId = correlationId,
                                )
                                call.respond(message = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock), status = HttpStatusCode.OK)
                            },
                        )
                    }
                }
            }
        }
    }
}
