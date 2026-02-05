package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
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
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        return behandling.settPåVent(
            endretAv = saksbehandler,
            begrunnelse = begrunnelse,
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
