package no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
import java.time.Clock

class DistribuerRammevedtaksbrevService(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val rammevedtakRepo: RammevedtakRepo,
    private val clock: Clock,
    private val korrelasjon: Korrelasjon,
) {
    private val log = KotlinLogging.logger {}

    /** Ment å kalles fra en jobb - journalfører alle rammevedtak som skal sende brev. */
    suspend fun distribuer() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                korrelasjon.medNyCorrelationId { correlationId ->
                    log.info { "Prøver å distribuere journalpost for rammevedtak. $vedtakSomSkalDistribueres" }
                    Either.catch {
                        val distribusjonId =
                            dokumentdistribusjonsklient.distribuerDokument(vedtakSomSkalDistribueres.journalpostId, correlationId)
                                .getOrElse {
                                    it.loggFeil(log, "distribuering av rammevedtaksbrev", "$vedtakSomSkalDistribueres")
                                    return@medNyCorrelationId
                                }
                        log.info { "Rammevedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
                        rammevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
                        log.info { "Rammevedtaksbrev markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
                    }.onLeft {
                        log.error(it) { "Feil ved distribuering av rammevedtaksbrev. $vedtakSomSkalDistribueres" }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av rammevedtaksbrev." }
        }
    }
}
