package no.nav.tiltakspenger.vedtak.routes.meldekort

import arrow.core.toNonEmptyListOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.ForMangeDagerUtfylt
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagFraSperret
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagTilSperret
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.KunneIkkeHenteSak
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.MåVæreSaksbehandler
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Dager
import no.nav.tiltakspenger.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBody
import no.nav.tiltakspenger.vedtak.routes.withMeldekortId
import no.nav.tiltakspenger.vedtak.routes.withSakId
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
        correlationId: CorrelationId,
    ): SendMeldekortTilBeslutningKommando {
        return SendMeldekortTilBeslutningKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            dager = Dager(
                this.dager.map { dag ->
                    Dager.Dag(
                        dag = LocalDate.parse(dag.dato),
                        status =
                        when (dag.status) {
                            "SPERRET" -> SendMeldekortTilBeslutningKommando.Status.SPERRET
                            "DELTATT_UTEN_LØNN_I_TILTAKET" -> SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                            "DELTATT_MED_LØNN_I_TILTAKET" -> SendMeldekortTilBeslutningKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                            "IKKE_DELTATT" -> SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
                            "FRAVÆR_SYK" -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYK
                            "FRAVÆR_SYKT_BARN" -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYKT_BARN
                            "FRAVÆR_VELFERD_GODKJENT_AV_NAV" -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                            "FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV" -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                            else -> throw IllegalArgumentException("Ukjent status: ${dag.status}")
                        },
                    )
                }.toNonEmptyListOrNull()!!,
            ),
            meldekortId = meldekortId,
        )
    }
}

fun Route.sendMeldekortTilBeslutterRoute(
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutningService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }
    post("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Mottatt post-request på /sak/{sakId}/meldekort/{meldekortId} - saksbehandler har fylt ut meldekortet og sendt til beslutter" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    call.withBody<Body> { body ->
                        val correlationId = call.correlationId()
                        val kommando = body.toDomain(
                            saksbehandler = saksbehandler,
                            meldekortId = meldekortId,
                            sakId = sakId,
                            correlationId = correlationId,
                        )
                        sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(kommando).fold(
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

                                    KanIkkeSendeMeldekortTilBeslutning.InnsendteDagerMåMatcheMeldeperiode -> call.respond400BadRequest(
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
                                call.respond(message = {}, status = HttpStatusCode.OK)
                            },
                        )
                    }
                }
            }
        }
    }
}
