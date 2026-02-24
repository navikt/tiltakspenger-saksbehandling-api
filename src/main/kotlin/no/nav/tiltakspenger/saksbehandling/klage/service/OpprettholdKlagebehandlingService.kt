package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.opprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class OpprettholdKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    suspend fun oppretthold(
        kommando: OpprettholdKlagebehandlingKommando,
    ): Either<KanIkkeOpprettholdeKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettholdKlagebehandling(kommando).onRight {
            val statistikk = statistikkSakService.genererSaksstatistikkForKlagebehandlingOversendtTilKabal(it.second)

            sessionFactory.withTransactionContext { tx ->
                klagebehandlingRepo.lagreKlagebehandling(it.second)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
    }
}
