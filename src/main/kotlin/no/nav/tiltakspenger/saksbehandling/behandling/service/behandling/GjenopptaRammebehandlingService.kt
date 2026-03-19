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
import java.time.Clock

class GjenopptaRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun gjenopptaBehandling(
        kommando: GjenopptaRammebehandlingKommando,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> =
        gjenopptaBehandlingInternal(kommando)

    /**
     * Brukes når klagebehandlingen gjenopptar rammebehandlingen.
     * Klagebehandlingen håndterer sin egen statistikk, så vi genererer ikke klagestatistikk her.
     */
    suspend fun gjenopptaBehandlingFraKlage(
        kommando: GjenopptaRammebehandlingKommando,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> =
        gjenopptaBehandlingInternal(kommando)

    private suspend fun gjenopptaBehandlingInternal(
        kommando: GjenopptaRammebehandlingKommando,
    ): Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>> {
        val (sakId, behandlingId, _, correlationId) = kommando
        val (sak, behandling) = behandlingService.hentSakOgRammebehandling(sakId, behandlingId)

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
        }.map { (oppdatertBehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertBehandling)
            behandlingService.lagreMedStatistikk(
                oppdatertBehandling,
                statistikkhendelser,
            )
            oppdatertSak to oppdatertBehandling
        }
    }
}

sealed interface KunneIkkeGjenopptaBehandling {
    data class FeilVedOppdateringAvSaksopplysninger(val originalFeil: KunneIkkeOppdatereSaksopplysninger) : KunneIkkeGjenopptaBehandling
}
