package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.KanIkkeSetteKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class SettKlagebehandlingPåVentService(
    private val sakService: SakService,
    private val settRammebehandlingPåVentService: SettRammebehandlingPåVentService,
    private val settMeldekortbehandlingPåVentService: SettMeldekortbehandlingPåVentService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    suspend fun settPåVent(
        kommando: SettKlagebehandlingPåVentKommando,
    ): Either<KanIkkeSetteKlagebehandlingPåVent, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.settKlagebehandlingPåVent(
            kommando = kommando,
            clock = clock,
            settRammebehandlingPåVent = settRammebehandlingPåVentService::settBehandlingPåVentFraKlage,
            settMeldekortbehandlingPåVent = { settPåVentKommando ->
                settMeldekortbehandlingPåVentService.settPåVent(settPåVentKommando).getOrElse {
                    throw IllegalStateException(
                        "Kunne ikke sette meldekortbehandling ${settPåVentKommando.meldekortId} tilknyttet klagebehandling ${kommando.klagebehandlingId} på vent: $it",
                    )
                }
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
