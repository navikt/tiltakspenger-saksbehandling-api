package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.LocalDateTime

class SettBehandlingPåVentService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun settBehandlingPåVent(
        behandlingId: BehandlingId,
        begrunnelse: String,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Behandling {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)

        return behandling.settPåVent(
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            tidspunkt = LocalDateTime.now(),
        ).also {
            val statistikk = statistikkSakService.genererStatistikkForBehandlingSattPåVent(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
    }
}
