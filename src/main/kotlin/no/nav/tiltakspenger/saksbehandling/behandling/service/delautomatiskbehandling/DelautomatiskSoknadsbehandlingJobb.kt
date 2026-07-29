package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import java.time.Clock

class DelautomatiskSoknadsbehandlingJobb(
    private val søknadRepo: SøknadRepo,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
    private val delautomatiskBehandlingService: DelautomatiskBehandlingService,
    private val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettSøknadsbehandlingerFraNyeSøknader() {
        val søknadIder = søknadRepo.hentUbehandledeSøknadIder(limit = 10)
        log.debug { "Fant ${søknadIder.size} åpne søknader som det skal opprettes behandling for" }
        søknadIder.forEach { opprettSøknadsbehandlingForSøknad(it) }
    }

    suspend fun opprettSøknadsbehandlingForSøknad(søknadId: SøknadId) {
        val correlationId = CorrelationId.generate()
        try {
            val søknad = søknadRepo.hentUbehandletSøknad(søknadId)
            if (søknad == null) {
                log.info { "Søknad med id $søknadId er ikke lenger ubehandlet, oppretter ikke automatisk behandling" }
                return
            }
            log.info { "Oppretter automatisk behandling for søknad med id $søknadId, correlationId $correlationId" }
            // Servicen logger selv ved Left, så vi hopper bare videre til neste søknad.
            val behandling = startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(søknad, correlationId)
                .getOrElse { return }
            log.info { "Opprettet behandling med id ${behandling.id} for søknad med id $søknadId, correlationId $correlationId" }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved oppretting av automatisk behandling for søknad med id $søknadId, correlationId $correlationId" }
        }
    }

    suspend fun automatiskBehandleSøknadsbehandlinger() {
        val behandlingIder = rammebehandlingRepo.hentAutomatiskeSoknadsbehandlingIder(limit = 10)
        log.debug { "Fant ${behandlingIder.size} åpne automatiske søknadsbehandlinger" }
        behandlingIder.forEach { automatiskBehandleSøknadsbehandling(it) }
    }

    suspend fun automatiskBehandleSøknadsbehandling(behandlingId: RammebehandlingId) {
        val correlationId = CorrelationId.generate()
        try {
            val behandling = rammebehandlingRepo.hent(behandlingId)
            if (behandling !is Søknadsbehandling || !behandling.erUnderAutomatiskBehandling) {
                log.info { "Behandling med id $behandlingId er ikke lenger under automatisk behandling, hopper over" }
                return
            }
            val venterTil = behandling.venterTil
            if (venterTil != null && venterTil >= nå(clock)) {
                log.info { "Behandling med id $behandlingId venter til $venterTil, hopper over" }
                return
            }
            log.info { "Starter behandling med id ${behandling.id} for søknad med id ${behandling.søknad.id}, correlationId $correlationId" }
            if (behandling.ventestatus.erSattPåVent) {
                log.info { "Oppdaterer saksopplysninger for behandling med id ${behandling.id}, correlationId $correlationId" }
                val (_, oppdatertBehandling) = oppdaterSaksopplysningerService.oppdaterSaksopplysninger(behandling.sakId, behandling.id, AUTOMATISK_SAKSBEHANDLER, correlationId).getOrElse {
                    log.error { "Kunne ikke oppdatere saksopplysninger for behandling med id ${behandling.id}" }
                    throw IllegalStateException("Kunne ikke oppdatere saksopplysninger")
                }
                delautomatiskBehandlingService.behandleAutomatisk(oppdatertBehandling as Søknadsbehandling, correlationId)
            } else {
                delautomatiskBehandlingService.behandleAutomatisk(behandling, correlationId)
            }
            log.info { "Ferdig med å behandle søknad med id ${behandling.søknad.id} og behandlingsid ${behandling.id}, correlationId $correlationId" }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved automatisk behandling av behandling med id $behandlingId, correlationId $correlationId" }
        }
    }
}
