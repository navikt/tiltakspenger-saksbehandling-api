package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.KanIkkeOvertaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overtaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OvertaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class OvertaKlagebehandlingService(
    private val sakService: SakService,
    private val overtaRammebehandlingService: OvertaRammebehandlingService,
    private val overtaMeldekortbehandlingService: OvertaMeldekortbehandlingService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun overta(
        kommando: OvertaKlagebehandlingKommando,
    ): Either<KanIkkeOvertaKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.overtaKlagebehandling(
            kommando = kommando,
            clock = clock,
            overtaRammebehandling = { overtaKommando ->
                overtaRammebehandlingService.overta(overtaKommando)
                    .mapLeft { KanIkkeOvertaKlagebehandling.KunneIkkeOvertaRammebehandling(it) }
            },
            overtaMeldekortbehandling = { overtaKommando ->
                overtaMeldekortbehandlingService.overta(overtaKommando)
                    .mapLeft { KanIkkeOvertaKlagebehandling.KunneIkkeOvertaMeldekortbehandling(it) }
            },
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
