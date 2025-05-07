package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class StartRevurderingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val saksopplysningerService: OppdaterSaksopplysningerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Either<KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
        val (sakId, correlationId, saksbehandler) = kommando

        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            when (it) {
                is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> {
                    logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til sak for sakId $sakId" }
                    return KanIkkeStarteRevurdering.HarIkkeTilgang(
                        kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
                        harRollene = saksbehandler.roller,
                    ).left()
                }
            }
        }

        sjekkSaksbehandlersTilgangTilPerson(sak.fnr, sak.id, saksbehandler, correlationId)

        val (oppdatertSak, behandling) = sak
            .startRevurdering(kommando, clock, saksopplysningerService::hentSaksopplysningerFraRegistre)
            .getOrElse { return it.left() }

        val statistikk = statistikkSakService.genererStatistikkForRevurdering(behandling)

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
        return Pair(oppdatertSak, behandling).right()
    }

    private suspend fun sjekkSaksbehandlersTilgangTilPerson(
        fnr: Fnr,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) {
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            )
            .onLeft { throw IkkeFunnetException("Feil ved sjekk av tilgang til person. SakId: $sakId. CorrelationId: $correlationId") }
            .onRight { if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person") }
    }
}
