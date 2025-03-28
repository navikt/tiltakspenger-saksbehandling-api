package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.distribuering

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.RammevedtakRepo
import java.time.Clock

class DistribuerVedtaksbrevService(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val rammevedtakRepo: RammevedtakRepo,
    private val clock: Clock,
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
                        dokumentdistribusjonsklient.distribuerDokument(vedtakSomSkalDistribueres.journalpostId, correlationId)
                            .getOrElse {
                                log.error { "Kunne ikke distribuere vedtaksbrev. $vedtakSomSkalDistribueres" }
                                return@forEach
                            }
                    log.info { "Vedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
                    rammevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
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
