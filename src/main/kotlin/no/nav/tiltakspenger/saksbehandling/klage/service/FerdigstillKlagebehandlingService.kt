package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.FerdigstillKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.KunneIkkeFerdigstilleKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.ferdigstillKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class FerdigstillKlagebehandlingService(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun ferdigstill(
        kommando: FerdigstillKlagebehandlingKommando,
    ): Either<KunneIkkeFerdigstilleKlagebehandling, Klagebehandling> {
        val sak = sakService.hentForSakId(kommando.sakId)

        return sak.ferdigstillKlagebehandling(kommando, clock).map { (oppdatertKlagebehandling, statistikkhendelser) ->
            val statistikkDTO = statistikkService.generer(statistikkhendelser)
            sessionFactory.withTransactionContext { tx ->
                klagebehandlingRepo.lagreKlagebehandling(oppdatertKlagebehandling, tx)
                statistikkService.lagre(statistikkDTO, tx)
            }
            oppdatertKlagebehandling
        }
    }
}
