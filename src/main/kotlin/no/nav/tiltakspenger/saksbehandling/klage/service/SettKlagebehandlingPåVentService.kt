package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SaksstatistikkRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.KanIkkeSetteKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkService
import java.time.Clock

class SettKlagebehandlingPåVentService(
    private val sakService: SakService,
    private val settRammebehandlingPåVentService: SettRammebehandlingPåVentService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val saksstatistikkService: SaksstatistikkService,
    private val saksstatistikkRepo: SaksstatistikkRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun settPåVent(
        kommando: SettKlagebehandlingPåVentKommando,
    ): Either<KanIkkeSetteKlagebehandlingPåVent, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.settKlagebehandlingPåVent(
            kommando = kommando,
            clock = clock,
            settRammebehandlingPåVent = settRammebehandlingPåVentService::settBehandlingPåVentFraKlage,
            lagreKlagebehandling = klagebehandlingRepo::lagreKlagebehandling,
        ).onRight {
            val statistikk = saksstatistikkService.genererSaksstatistikkForKlagebehandlingSattPåVent(it.second)

            sessionFactory.withTransactionContext { tx ->
                klagebehandlingRepo.lagreKlagebehandling(it.second)
                saksstatistikkRepo.lagre(statistikk, tx)
            }
        }
    }
}
