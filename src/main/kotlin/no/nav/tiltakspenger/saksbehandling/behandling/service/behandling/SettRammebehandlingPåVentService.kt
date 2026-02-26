package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class SettRammebehandlingPåVentService(
    private val behandlingService: RammebehandlingService,
    private val statistikkSakService: StatistikkSakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun settBehandlingPåVent(
        kommando: SettRammebehandlingPåVentKommando,
    ): Pair<Sak, Rammebehandling> = settBehandlingPåVentInternal(kommando, genererKlageStatistikk = true)

    /**
     * Brukes når klagebehandlingen setter rammebehandlingen på vent.
     * Klagebehandlingen håndterer sin egen statistikk, så vi genererer ikke klagestatistikk her.
     */
    suspend fun settBehandlingPåVentFraKlage(
        kommando: SettRammebehandlingPåVentKommando,
    ): Pair<Sak, Rammebehandling> = settBehandlingPåVentInternal(kommando, genererKlageStatistikk = false)

    private suspend fun settBehandlingPåVentInternal(
        kommando: SettRammebehandlingPåVentKommando,
        genererKlageStatistikk: Boolean,
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = kommando.sakId,
            behandlingId = kommando.rammebehandlingId,
        )

        return behandling.settPåVent(
            kommando = kommando,
            clock = clock,
        ).let { behandling ->
            val oppdaterSak = sak.oppdaterRammebehandling(behandling)

            behandlingService.lagreMedStatistikk(
                behandling = behandling,
                statistikk = statistikkSakService.genererStatistikkForBehandlingSattPåVent(behandling),
                klageStatistikk = if (genererKlageStatistikk) {
                    behandling.klagebehandling?.let {
                        statistikkSakService.genererSaksstatistikkForKlagebehandlingSattPåVent(it)
                    }
                } else {
                    null
                },
            )

            oppdaterSak to behandling
        }
    }
}
