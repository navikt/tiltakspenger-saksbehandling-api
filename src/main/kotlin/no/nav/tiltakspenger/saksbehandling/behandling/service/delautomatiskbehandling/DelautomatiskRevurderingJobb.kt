package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat

/**
 * Plukker opp revurderinger under automatisk behandling og forsøker å behandle dem.
 * Stanser behandles av [DelautomatiskStansService.forsøkAutomatiskStans], og forlengelser av [DelautomatiskForlengelseService.forsøkAutomatiskForlengelse].
 * Feiler en revurdering, logges feilen, og revurderingen forsøkes på nytt neste gang jobben kjører.
 */
class DelautomatiskRevurderingJobb(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val delautomatiskStansService: DelautomatiskStansService,
    private val delautomatiskForlengelseService: DelautomatiskForlengelseService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun automatiskBehandleRevurderinger() {
        val revurderingIder = rammebehandlingRepo.hentAutomatiskeRevurderingIder(limit = 10)
        log.debug { "Fant ${revurderingIder.size} revurderinger under automatisk behandling" }
        revurderingIder.forEach { automatiskBehandleRevurdering(it) }
    }

    suspend fun automatiskBehandleRevurdering(revurderingId: RammebehandlingId) {
        val correlationId = CorrelationId.generate()
        try {
            val revurdering = rammebehandlingRepo.hent(revurderingId)
            when (revurdering.resultat) {
                is Revurderingsresultat.Stans -> delautomatiskStansService.forsøkAutomatiskStans(revurdering.sakId, revurdering.id, correlationId)
                is Revurderingsresultat.Innvilgelse -> delautomatiskForlengelseService.forsøkAutomatiskForlengelse(revurdering.sakId, revurdering.id, correlationId)
                else -> log.error { "Revurdering med id $revurderingId og resultat ${revurdering.resultat?.let { it::class.simpleName }} støttes ikke av automatisk behandling, correlationId $correlationId" }
            }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved automatisk behandling av revurdering med id $revurderingId, correlationId $correlationId" }
        }
    }
}
