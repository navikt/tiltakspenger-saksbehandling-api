package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import java.time.Clock

class DistribuerKlagevedtaksbrevService(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    /** Ment å kalles fra en jobb - journalfører alle klagevedtak (avvisninger) som skal sende brev. */
    suspend fun distribuer() {
        Either.catch {
            klagevedtakRepo.hentKlagevedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                val correlationId = CorrelationId.generate()
                log.info { "Prøver å distribuere journalpost for klagevedtak. $vedtakSomSkalDistribueres" }
                Either.catch {
                    val distribusjonId =
                        dokumentdistribusjonsklient.distribuerDokument(vedtakSomSkalDistribueres.journalpostId, correlationId)
                            .getOrElse {
                                log.error { "Kunne ikke distribuere vedtaksbrev. $vedtakSomSkalDistribueres" }
                                return@forEach
                            }
                    log.info { "Klagevedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
                    klagevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
                    log.info { "Klagevedtaksbrev markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
                }.onLeft {
                    log.error(it) { "Feil ved distribuering av klagevedtaksbrev. $vedtakSomSkalDistribueres" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av klagevedtaksbrev." }
        }
    }
}
