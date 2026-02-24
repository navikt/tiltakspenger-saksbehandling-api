package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.KanIkkeGjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class GjenopptaKlagebehandlingService(
    private val sakService: SakService,
    private val gjenopptaRammebehandlingService: GjenopptaRammebehandlingService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun gjenoppta(
        kommando: GjenopptaKlagebehandlingKommando,
    ): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.gjenopptaKlagebehandling(
            kommando = kommando,
            clock = clock,
            gjenopptaRammebehandling = gjenopptaRammebehandlingService::gjenopptaBehandling,
            lagreKlagebehandling = ::lagreKlageOgStatistikk,
        )
    }

    suspend fun lagreKlageOgStatistikk(
        klagebehandling: Klagebehandling,
    ) {
        val statistikk = statistikkSakService.genererSaksstatistikkForGjenopptattKlagebehandling(klagebehandling)

        sessionFactory.withTransactionContext { tx ->
            klagebehandlingRepo.lagreKlagebehandling(klagebehandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
    }
}
