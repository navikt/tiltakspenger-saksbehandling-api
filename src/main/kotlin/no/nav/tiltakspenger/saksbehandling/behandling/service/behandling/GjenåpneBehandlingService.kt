package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
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
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteSøknadsbehandling, Søknadsbehandling> {
        val sak = sakService.sjekkTilgangOgHentForSakId(sakId, saksbehandler, correlationId)
        val behandling = behandlingRepo.hentForSøknadId(søknadId)

        if (behandling == null) {
            throw IllegalStateException("Ingen behandling funnet for søknadId: $søknadId")
        }

        when (behandling) {
            is Søknadsbehandling -> {
                if (!behandling.erVedtatt || behandling.utfall is SøknadsbehandlingResultat.Innvilgelse) {
                    throw IllegalStateException("Kan ikke gjenåpne en behandling som ikke er vedtatt eller har et utfall som er innvilget. BehandlingId: ${behandling.id}")
                }
                val vurderingsperiode = behandling.søknad.vurderingsperiode()
                val innvilgedePerioder = sak.vedtaksliste.innvilgetTidslinje.overlapperMed(vurderingsperiode)
                if (innvilgedePerioder.perioderMedVerdi.isNotEmpty()) {
                    throw IllegalStateException(
                        """
                            Kan ikke gjenåpne en behandling som overlapper med en periode som allerede er innvilget.
                            BehandlingId: ${behandling.id}, innvilgedePerioder: $innvilgedePerioder""",
                    )
                }

                return startSøknadsbehandlingService.startSøknadsbehandling(
                    søknadId = søknadId,
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
