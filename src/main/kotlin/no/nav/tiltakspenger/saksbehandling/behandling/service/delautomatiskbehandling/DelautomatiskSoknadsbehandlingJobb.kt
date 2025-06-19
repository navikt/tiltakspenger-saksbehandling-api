package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService

class DelautomatiskSoknadsbehandlingJobb(
    private val søknadRepo: SøknadRepo,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
    private val delautomatiskBehandlingService: DelautomatiskBehandlingService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun behandleNyeSoknader() {
        val ubehandledeSoknader = søknadRepo.hentAlleUbehandledeSoknader(limit = 10)
        log.info { "Fant ${ubehandledeSoknader.size} åpne søknader som det skal opprettes behandling for" }
        ubehandledeSoknader.forEach {
            log.info { "Starter behandling av søknad med id ${it.id}" }
            val correlationId = CorrelationId.generate()
            val behandling = startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(it, correlationId)
            log.info { "Opprettet behandling med id ${behandling.id} for søknad med id ${it.id}" }
            delautomatiskBehandlingService.behandleAutomatisk(behandling, correlationId)
            log.info { "Ferdig med å behandle søknad med id ${it.id}" }
        }
    }
}
