package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.KanIkkeGjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GjenopptaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class GjenopptaKlagebehandlingService(
    private val sakService: SakService,
    private val gjenopptaRammebehandlingService: GjenopptaRammebehandlingService,
    private val gjenopptaMeldekortbehandlingService: GjenopptaMeldekortbehandlingService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun gjenoppta(
        kommando: GjenopptaKlagebehandlingKommando,
    ): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.gjenopptaKlagebehandling(
            kommando = kommando,
            clock = clock,
            gjenopptaRammebehandling = { gjenopptaKommando ->
                gjenopptaRammebehandlingService.gjenopptaBehandlingFraKlage(gjenopptaKommando).getOrThrow()
            },
            gjenopptaMeldekortbehandling = gjenopptaMeldekortbehandlingService::gjenoppta,
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
