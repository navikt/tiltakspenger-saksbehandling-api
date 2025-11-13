package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class GjenopptaBehandlingService(
    private val behandlingService: BehandlingService,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkSakService: StatistikkSakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun gjenopptaBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        val gjenopptattBehandling = behandling.gjenoppta(saksbehandler, clock)

        val nyeSaksopplysninger = hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            tiltaksdeltagelserDetErSøktTiltakspengerFor = sak.tiltaksdeltagelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltagelserForBehandlingen = when (behandling) {
                is Revurdering -> sak.tiltaksdeltagelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.id }
                is Søknadsbehandling -> listOfNotNull(behandling.søknad.tiltak?.id)
            },
            inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = when (behandling) {
                is Revurdering -> false
                is Søknadsbehandling -> true
            },
        )

        return gjenopptattBehandling.oppdaterSaksopplysninger(saksbehandler, nyeSaksopplysninger).mapLeft {
            KunneIkkeGjenopptaBehandling.FeilVedOppdateringAvSaksopplysninger(it)
        }.map {
            val oppdatertSak = sak.oppdaterRammebehandling(it)

            behandlingService.lagreMedStatistikk(
                it,
                statistikkSakService.genererStatistikkForGjenopptattBehandling(it),
            )

            oppdatertSak to it
        }
    }
}

sealed interface KunneIkkeGjenopptaBehandling {
    data class FeilVedOppdateringAvSaksopplysninger(val originalFeil: KunneIkkeOppdatereSaksopplysninger) : KunneIkkeGjenopptaBehandling
}
