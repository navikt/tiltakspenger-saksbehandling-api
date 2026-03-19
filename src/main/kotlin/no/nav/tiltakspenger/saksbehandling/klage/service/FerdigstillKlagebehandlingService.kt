package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.FerdigstillKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.KunneIkkeFerdigstilleKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.ferdigstill
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class FerdigstillKlagebehandlingService(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun ferdigstill(
        kommando: FerdigstillKlagebehandlingKommando,
    ): Either<KunneIkkeFerdigstilleKlagebehandling, Klagebehandling> {
        val klagebehandling = klagebehandlingRepo.hentForKlagebehandlingId(kommando.klagebehandlingId)!!

        return klagebehandling.ferdigstill(kommando, clock).map { (oppdatertKlagebehandling, statistikkhendelser) ->
            val statistikkDTO = statistikkService.generer(statistikkhendelser)
            sessionFactory.withTransactionContext { tx ->
                klagebehandlingRepo.lagreKlagebehandling(oppdatertKlagebehandling, tx)
                statistikkService.lagre(statistikkDTO, tx)
            }
            oppdatertKlagebehandling
        }
    }
}
