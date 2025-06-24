package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService

class DelautomatiskSoknadsbehandlingJobb(
    private val søknadRepo: SøknadRepo,
    private val behandlingRepo: BehandlingRepo,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
    private val delautomatiskBehandlingService: DelautomatiskBehandlingService,
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
        val automatiskeBehandlinger = behandlingRepo.hentAlleAutomatiskeSoknadsbehandlinger(limit = 10)
        log.debug { "Fant ${automatiskeBehandlinger.size} åpne automatiske søknadsbehandlinger" }
        automatiskeBehandlinger.forEach {
            val correlationId = CorrelationId.generate()
            try {
                log.info { "Starter behandling med id ${it.id} for søknad med id ${it.søknad.id}, correlationId $correlationId" }
                delautomatiskBehandlingService.behandleAutomatisk(it, correlationId)
                log.info { "Ferdig med å behandle søknad med id ${it.søknad.id} og behandlingsid ${it.id}, correlationId $correlationId" }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved automatisk behandling av behandling med id ${it.id}, correlationId $correlationId" }
            }
        }
    }
}
