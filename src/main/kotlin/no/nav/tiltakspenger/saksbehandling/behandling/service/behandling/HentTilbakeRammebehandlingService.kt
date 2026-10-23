package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake.KanIkkeHenteTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake.hentTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class HentTilbakeRammebehandlingService(
    private val rammebehandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
    private val clock: Clock,
) {
    suspend fun hentTilbakeRammebehandling(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Either<KanIkkeHenteTilbakeRammebehandling, Pair<Sak, Rammebehandling>> {
        val (sak, behandling) = rammebehandlingService.hentSakOgRammebehandling(sakId, behandlingId)

        return behandling.hentTilbakeRammebehandling(saksbehandler, clock).map { (oppdatertRammebehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
            val statistikkDTO = statistikkService.generer(statistikkhendelser)

            val vellykket = sessionFactory.withTransactionContext { tx ->
                val oppdatert = rammebehandlingRepo.hentTilbake(
                    rammebehandling = oppdatertRammebehandling,
                    forventetSendtTilBeslutning = behandling.sendtTilBeslutning,
                    transactionContext = tx,
                )
                if (oppdatert) statistikkService.lagre(statistikkDTO, tx)
                oppdatert
            }

            if (!vellykket) return KanIkkeHenteTilbakeRammebehandling.BehandlingenErEndret.left()

            oppdatertSak to oppdatertRammebehandling
        }
    }
}
