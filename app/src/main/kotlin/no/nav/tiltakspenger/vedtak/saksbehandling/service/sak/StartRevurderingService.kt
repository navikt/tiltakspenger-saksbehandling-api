package no.nav.tiltakspenger.vedtak.saksbehandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.vedtak.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.vedtak.felles.exceptions.TilgangException
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.startRevurdering
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.OppdaterSaksopplysningerService

class StartRevurderingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val saksopplysningerService: OppdaterSaksopplysningerService,
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
            .startRevurdering(kommando, saksopplysningerService::hentSaksopplysningerFraRegistre)
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
