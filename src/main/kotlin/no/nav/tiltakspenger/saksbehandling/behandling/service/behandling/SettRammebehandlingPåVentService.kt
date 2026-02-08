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
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = kommando.sakId,
            behandlingId = kommando.rammebehandlingId,
        )

        return behandling.settPåVent(
            kommando = kommando,
            clock = clock,
        ).let {
            val oppdaterSak = sak.oppdaterRammebehandling(it)

            behandlingService.lagreMedStatistikk(
                it,
                statistikkSakService.genererStatistikkForBehandlingSattPåVent(it),
            )

            oppdaterSak to it
        }
    }
}
