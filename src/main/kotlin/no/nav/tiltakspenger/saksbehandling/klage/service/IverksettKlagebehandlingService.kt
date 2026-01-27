package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
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
     * Når vi skal iverksette en rammebehandling fra klage, så må vi vurdere om vi skal lage et vedtak eller om vi kun skal ferdigstille klagebehandlingen uten vedtak.
     */
    suspend fun iverksett(
        kommando: IverksettKlagebehandlingKommando,
    ): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagevedtak>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.iverksettKlagebehandling(kommando, clock).onRight {
            sessionFactory.withTransactionContext { tc ->
                klagebehandlingRepo.lagreKlagebehandling(it.second.behandling, tc)
                klagevedtakRepo.lagreVedtak(it.second, tc)
            }
        }
    }
}
