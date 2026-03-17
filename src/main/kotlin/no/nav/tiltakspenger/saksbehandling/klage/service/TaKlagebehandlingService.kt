package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.taKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class TaKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val taRammebehandlingService: TaRammebehandlingService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    suspend fun ta(
        kommando: TaKlagebehandlingKommando,
    ): Either<KanIkkeTaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.taKlagebehandling(
            kommando = kommando,
            sistEndret = nå(clock),
            taRammebehandling = taRammebehandlingService::taBehandling,
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
