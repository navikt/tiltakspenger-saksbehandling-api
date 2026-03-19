package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.KanIkkeLeggeTilbakeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.LeggTilbakeKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbakeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class LeggTilbakeKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val leggTilbakeRammebehandlingService: LeggTilbakeRammebehandlingService,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun leggTilbake(
        kommando: LeggTilbakeKlagebehandlingKommando,
    ): Either<KanIkkeLeggeTilbakeKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.leggTilbakeKlagebehandling(
            kommando = kommando,
            clock = clock,
            leggTilbakeRammebehandling = leggTilbakeRammebehandlingService::leggTilbakeRammebehandling,
            lagre = ::lagreKlagebehandlingOgStatistikk,
        )
    }

    private suspend fun lagreKlagebehandlingOgStatistikk(
        klagebehandling: Klagebehandling,
        statistikkhendelser: Statistikkhendelser,
    ) {
        val statistikkDTO = statistikkService.generer(statistikkhendelser)
        sessionFactory.withTransactionContext { tx ->
            klagebehandlingRepo.lagreKlagebehandling(klagebehandling, tx)
            statistikkService.lagre(statistikkDTO, tx)
        }
    }
}
