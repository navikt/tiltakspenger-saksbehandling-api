package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import java.time.Clock

class DistribuerMeldekortvedtaksbrevJobb(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val clock: Clock,
    private val korrelasjon: Korrelasjon,
) {
    private val log = KotlinLogging.logger {}

    suspend fun distribuer() {
        Either.catch {
            meldekortvedtakRepo.hentMeldekortvedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                korrelasjon.medNyCorrelationId { correlationId ->
                    log.info { "Prøver å distribuere journalpost for meldekortvedtak. $vedtakSomSkalDistribueres" }
                    Either.catch {
                        val distribusjonId =
                            dokumentdistribusjonsklient.distribuerDokument(vedtakSomSkalDistribueres.journalpostId, correlationId)
                                .getOrElse {
                                    it.loggFeil(log, "distribuering av meldekortvedtaksbrev", "$vedtakSomSkalDistribueres")
                                    return@medNyCorrelationId
                                }
                        meldekortvedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
                        log.info { "Meldekortvedtaksbrev distribuert og markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
                    }.onLeft {
                        log.error(it) { "Feil ved distribuering av meldekortvedtaksbrev. $vedtakSomSkalDistribueres" }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av meldekortvedtaksbrev." }
        }
    }
}

fun (() -> DistribuerMeldekortvedtaksbrevJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-distribuer-meldekortvedtaksbrev",
            utfør = { _ ->
                this().distribuer()
                TaskResultat.Ferdig
            },
        ),
    )
}
