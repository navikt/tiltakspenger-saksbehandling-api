package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class GjenåpneBehandlingService(
    private val clock: Clock,
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val sakService: SakService,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun gjenåpneBehandling(
        behandlingId: BehandlingId,
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteSøknadsbehandling, Søknadsbehandling> {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)

        when (behandling) {
            is Søknadsbehandling -> {
                val søknad = behandling.søknad
//                søknad.

                return startSøknadsbehandlingService.startSøknadsbehandling(
                    søknadId = søknad.id,
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                )
            }

            is Revurdering -> {
                throw IllegalStateException("Kan ikke gjenåpne en revurdering. BehandlingId: ${behandling.id}")
            }
        }
    }
}
