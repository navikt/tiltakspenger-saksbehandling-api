package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class IverksettKlagebehandlingService(
    private val sakService: SakService,
    private val clock: Clock,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val sessionFactory: SessionFactory,
) {
    /**
     * Skal i utgangspunktet kun iverksette avviste klager.
     * Når vi skal iverksette en rammebehandling fra klage, bruker vi endepunktet/service for rammebehandling.
     */
    fun iverksett(
        kommando: IverksettKlagebehandlingKommando,
    ): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagevedtak>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.iverksettKlagebehandling(kommando, clock).onRight {
            sessionFactory.withTransactionContext { transactionContext ->
                // Obs: Dersom du endrer eller legger til noe her, merk at det kan hende du må gjøre tilsvarende i [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService] Eksempel kan være statistikk, metrikker.
                klagebehandlingRepo.lagreKlagebehandling(it.second.behandling, transactionContext)
                klagevedtakRepo.lagreVedtak(it.second, transactionContext)
            }
        }
    }
}
