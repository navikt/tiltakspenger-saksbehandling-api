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
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class StartRevurderingService(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    private val behandlingRepo: BehandlingRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val saksopplysningerService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
        val (sakId, correlationId, saksbehandler) = kommando

        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            when (it) {
                is no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId.HarIkkeTilgang -> {
                    logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til sak for sakId $sakId" }
                    return no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteRevurdering.HarIkkeTilgang(
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

        behandlingRepo.lagre(behandling)
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
