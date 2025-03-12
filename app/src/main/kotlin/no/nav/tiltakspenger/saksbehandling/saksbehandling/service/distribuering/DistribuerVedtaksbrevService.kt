package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.distribuering

import arrow.core.Either
import arrow.core.getOrElse
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.distribusjon.DokdistGateway
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.RammevedtakRepo

class DistribuerVedtaksbrevService(
    private val dokdistGateway: DokdistGateway,
    private val rammevedtakRepo: RammevedtakRepo,
) {
    private val log = KotlinLogging.logger {}

    /** Ment å kalles fra en jobb - journalfører alle rammevedtak som skal sende brev. */
    suspend fun distribuer() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                val correlationId = CorrelationId.generate()
                log.info { "Prøver å distribuere journalpost  for rammevedtak. $vedtakSomSkalDistribueres" }
                Either.catch {
                    val distribusjonId =
                        dokdistGateway.distribuerDokument(vedtakSomSkalDistribueres.journalpostId, correlationId)
                            .getOrElse {
                                log.error { "Kunne ikke distribuere vedtaksbrev. $vedtakSomSkalDistribueres" }
                                return@forEach
                            }
                    log.info { "Vedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
                    rammevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå())
                    log.info { "Vedtaksbrev markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
                }.onLeft {
                    log.error(it) { "Feil ved journalføring av vedtaksbrev. $vedtakSomSkalDistribueres" }
                }
            }
        }.onLeft {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil skjedde under distribuering av førstegangsvedtak." }
            sikkerlogg.error(it) { "Ukjent feil skjedde under distribuering av førstegangsvedtak." }
        }
    }
}
