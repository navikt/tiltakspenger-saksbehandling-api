package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService

class AvbrytKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    /**
     * Kan ikke avbrytes dersom den er tilknyttet en [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling]
     */
    suspend fun avbrytKlagebehandling(
        kommando: AvbrytKlagebehandlingKommando,
    ): Either<KanIkkeAvbryteKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.avbrytKlagebehandling(
            kommando = kommando,
            clock = clock,
        ).map { (oppdatertSak, oppdatertKlagebehandling, statistikkhendelser) ->
            val statistikkDTO = statistikkService.generer(statistikkhendelser)
            sessionFactory.withTransactionContext { tx ->
                klagebehandlingRepo.lagreKlagebehandling(oppdatertKlagebehandling, tx)
                statistikkService.lagre(statistikkDTO, tx)
            }
            oppdatertSak to oppdatertKlagebehandling
        }
    }
}
