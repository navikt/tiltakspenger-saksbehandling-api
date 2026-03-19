package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class SettRammebehandlingPåVentService(
    private val behandlingService: RammebehandlingService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun settBehandlingPåVent(
        kommando: SettRammebehandlingPåVentKommando,
    ): Pair<Sak, Rammebehandling> = settRammebehandlingPåVentInternal(kommando)

    /**
     * Brukes når klagebehandlingen setter rammebehandlingen på vent.
     * Klagebehandlingen håndterer sin egen statistikk, så vi genererer ikke klagestatistikk her.
     */
    suspend fun settBehandlingPåVentFraKlage(
        kommando: SettRammebehandlingPåVentKommando,
    ): Pair<Sak, Rammebehandling> = settRammebehandlingPåVentInternal(kommando)

    private suspend fun settRammebehandlingPåVentInternal(
        kommando: SettRammebehandlingPåVentKommando,
    ): Pair<Sak, Rammebehandling> {
        val (sak, rammebehandling) = behandlingService.hentSakOgRammebehandling(
            sakId = kommando.sakId,
            behandlingId = kommando.rammebehandlingId,
        )
        return rammebehandling.settPåVent(
            kommando = kommando,
            clock = clock,
        ).let { (oppdatertRammebehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)

            behandlingService.lagreMedStatistikk(
                behandling = oppdatertRammebehandling,
                statistikkhendelser = statistikkhendelser,
            )
            oppdatertSak to oppdatertRammebehandling
        }
    }
}
