package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import arrow.core.toNonEmptyListOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortbehandlingService
import java.time.Clock
import java.time.LocalDate

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/oppdater"

fun Route.oppdaterMeldekortbehandlingRoute(
    oppdaterMeldekortbehandlingService: OppdaterMeldekortbehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler har oppdatert et meldekort under behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<OppdaterMeldekortbehandlingBody> { body ->
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    val correlationId = call.correlationId()

                    oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                        kommando = body.toDomain(
                            saksbehandler = saksbehandler,
                            meldekortId = meldekortId,
                            sakId = sakId,
                            correlationId = correlationId,
                        ),
                        clock = clock,
                    ).fold(
                        ifLeft = {
                            respondWithError(it)
                        },
                        ifRight = {
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Saksbehandler har oppdatert et meldekort under behandling",
                                correlationId = correlationId,
                            )
                            call.respondJson(
                                value = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock),
                            )
                        },
                    )
                }
            }
        }
    }
}

suspend fun RoutingContext.respondWithError(meldekort: KanIkkeOppdatereMeldekortbehandling) {
    when (meldekort) {
        is MeldekortperiodenKanIkkeVæreFremITid -> {
            call.respond400BadRequest(
                melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
            )
        }
    }
}

private data class OppdaterMeldekortbehandlingBody(
    val meldeperioder: List<Meldeperiode>,
    val begrunnelse: String?,
    val tekstTilVedtaksbrev: String?,
    val skalSendeVedtaksbrev: Boolean,
) {

    data class Meldeperiode(
        val dager: List<Dag>,
        val kjedeId: String,
    )

    data class Dag(
        val dato: LocalDate,
        val status: OppdaterMeldekortbehandlingKommando.Status,
    )

    fun toDomain(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        sakId: SakId,
        correlationId: CorrelationId,
    ): OppdaterMeldekortbehandlingKommando {
        return OppdaterMeldekortbehandlingKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            meldeperioder = this.meldeperioder.map { mp ->
                OppdatertMeldeperiode(
                    kjedeId = MeldeperiodeKjedeId(mp.kjedeId),
                    dager = mp.dager.map { dag ->
                        OppdatertDag(
                            dag = dag.dato,
                            status = dag.status,
                        )
                    }.toNonEmptyListOrThrow(),
                )
            }.toNonEmptyListOrThrow(),
            meldekortId = meldekortId,
            begrunnelse = begrunnelse?.let { Begrunnelse.create(it) },
            fritekstTilVedtaksbrev = tekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }
}
