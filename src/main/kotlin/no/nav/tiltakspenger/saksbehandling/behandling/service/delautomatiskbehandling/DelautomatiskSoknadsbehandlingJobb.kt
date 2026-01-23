package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService

class DelautomatiskSoknadsbehandlingJobb(
    private val søknadRepo: SøknadRepo,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
    private val delautomatiskBehandlingService: DelautomatiskBehandlingService,
    private val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettBehandlingForNyeSoknader() {
        val ubehandledeSoknader = søknadRepo.hentAlleUbehandledeSoknader(limit = 10)
        log.debug { "Fant ${ubehandledeSoknader.size} åpne søknader som det skal opprettes behandling for" }
        ubehandledeSoknader.forEach {
            val correlationId = CorrelationId.generate()
            try {
                log.info { "Oppretter automatisk behandling for søknad med id ${it.id}, correlationId $correlationId" }
                val behandling = startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(it, correlationId)
                log.info { "Opprettet behandling med id ${behandling.id} for søknad med id ${it.id}, correlationId $correlationId" }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved oppretting av automatisk behandling for søknad med id ${it.id}, correlationId $correlationId" }
            }
        }
    }

    suspend fun behandleSoknaderAutomatisk() {
        val automatiskeBehandlinger = rammebehandlingRepo.hentAlleAutomatiskeSoknadsbehandlinger(limit = 10)
        log.debug { "Fant ${automatiskeBehandlinger.size} åpne automatiske søknadsbehandlinger" }
        automatiskeBehandlinger.forEach { behandling ->
            val correlationId = CorrelationId.generate()
            try {
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
                log.error(e) { "Noe gikk galt ved automatisk behandling av behandling med id ${behandling.id}, correlationId $correlationId" }
            }
        }
    }
}
