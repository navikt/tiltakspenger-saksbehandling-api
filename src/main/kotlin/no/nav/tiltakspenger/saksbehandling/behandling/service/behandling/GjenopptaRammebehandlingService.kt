package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.gjenoppta
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class GjenopptaRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkSakService: StatistikkSakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun gjenopptaBehandling(
        kommando: GjenopptaRammebehandlingKommando,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> =
        gjenopptaBehandlingInternal(kommando, genererKlageStatistikk = true)

    /**
     * Brukes når klagebehandlingen gjenopptar rammebehandlingen.
     * Klagebehandlingen håndterer sin egen statistikk, så vi genererer ikke klagestatistikk her.
     */
    suspend fun gjenopptaBehandlingFraKlage(
        kommando: GjenopptaRammebehandlingKommando,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> =
        gjenopptaBehandlingInternal(kommando, genererKlageStatistikk = false)

    private suspend fun gjenopptaBehandlingInternal(
        kommando: GjenopptaRammebehandlingKommando,
        genererKlageStatistikk: Boolean,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> {
        val (sakId, behandlingId, saksbehandler, correlationId) = kommando
        val (sak, behandling) = behandlingService.hentSakOgBehandling(sakId, behandlingId)

        val hentSaksopplysninger = suspend {
            hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                fnr = sak.fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = when (behandling) {
                    is Revurdering -> sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }
                    is Søknadsbehandling -> listOfNotNull(behandling.søknad.tiltak?.tiltaksdeltakerId)
                },
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = when (behandling) {
                    is Revurdering -> false
                    is Søknadsbehandling -> true
                },
            )
        }

        return behandling.gjenoppta(kommando, clock, hentSaksopplysninger).mapLeft {
            KunneIkkeGjenopptaBehandling.FeilVedOppdateringAvSaksopplysninger(it)
        }.map { behandling ->
            val oppdatertSak = sak.oppdaterRammebehandling(behandling)

            behandlingService.lagreMedStatistikk(
                behandling,
                statistikkSakService.genererStatistikkForGjenopptattBehandling(behandling),
                klageStatistikk = if (genererKlageStatistikk) {
                    behandling.klagebehandling?.let {
                        statistikkSakService.genererSaksstatistikkForGjenopptattKlagebehandling(it)
                    }
                } else {
                    null
                },
            )
            oppdatertSak to behandling
        }
    }
}

sealed interface KunneIkkeGjenopptaBehandling {
    data class FeilVedOppdateringAvSaksopplysninger(val originalFeil: KunneIkkeOppdatereSaksopplysninger) : KunneIkkeGjenopptaBehandling
}
