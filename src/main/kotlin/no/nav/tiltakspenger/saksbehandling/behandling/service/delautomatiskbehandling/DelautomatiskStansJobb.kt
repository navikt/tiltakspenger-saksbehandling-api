package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo

/**
 * Plukker opp stanser under automatisk behandling og forsøker å behandle dem, se [DelautomatiskStansService.forsøkAutomatiskStans].
 * Feiler en stans, logges feilen, og stansen forsøkes på nytt neste gang jobben kjører.
 */
class DelautomatiskStansJobb(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val delautomatiskStansService: DelautomatiskStansService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun automatiskBehandleStanser() {
        val revurderingIder = rammebehandlingRepo.hentAutomatiskeRevurderingIder(limit = 10)
        log.debug { "Fant ${revurderingIder.size} revurderinger under automatisk behandling" }
        revurderingIder.forEach { automatiskBehandleStans(it) }
    }

    private suspend fun automatiskBehandleStans(revurderingId: RammebehandlingId) {
        val correlationId = CorrelationId.generate()
        try {
            val revurdering = rammebehandlingRepo.hent(revurderingId)
            delautomatiskStansService.forsøkAutomatiskStans(revurdering.sakId, revurdering.id, correlationId)
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved automatisk behandling av stans med id $revurderingId, correlationId $correlationId" }
        }
    }
}
