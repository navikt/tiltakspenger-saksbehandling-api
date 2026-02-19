package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import java.time.Clock

class DistribuerKlagebrevJobb(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun distribuerAvvisningsbrev() {
        Either.catch {
            klagevedtakRepo.hentKlagevedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                val correlationId = CorrelationId.generate()
                log.info { "Prøver å distribuere journalpost for avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
                Either.catch {
                    val distribusjonId =
                        dokumentdistribusjonsklient.distribuerDokument(
                            vedtakSomSkalDistribueres.journalpostId,
                            correlationId,
                        )
                            .getOrElse {
                                log.error { "Kunne ikke distribuere avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
                                return@forEach
                            }
                    log.info { "Avvist klagevedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
                    klagevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
                    log.info { "Avvist klagevedtaksbrev markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
                }.onLeft {
                    log.error(it) { "Feil ved distribuering av avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av klagevedtaksbrev." }
        }
    }

    suspend fun distribuerInnstillingsbrev() {
        Either.Companion.catch {
            klagebehandlingRepo.hentInnstillingsbrevSomSkalDistribueres().forEach { behandlingSomSkalDistribueres ->
                val correlationId = CorrelationId.Companion.generate()
                val journalpostId: JournalpostId =
                    (behandlingSomSkalDistribueres.resultat as Klagebehandlingsresultat.Opprettholdt).journalpostIdInnstillingsbrev!!
                val kontekstTilLog =
                    "sakId: ${behandlingSomSkalDistribueres.sakId}, saksnummer: ${behandlingSomSkalDistribueres.saksnummer}, klagebehandlingId: ${behandlingSomSkalDistribueres.id}, journalpostId: $journalpostId"
                log.info { "Prøver å distribuere innstillingsbrev. $kontekstTilLog" }
                Either.Companion.catch {
                    val distribusjonId =
                        dokumentdistribusjonsklient.distribuerDokument(journalpostId, correlationId).getOrElse {
                            log.error { "Kunne ikke distribuere innstillingsbrev. Underliggende feil: $it. $kontekstTilLog" }
                            return@forEach
                        }
                    log.info { "Innstillingsbrev distribuert. distribusjonId: $distribusjonId, $kontekstTilLog" }
                    klagebehandlingRepo.lagreKlagebehandling(
                        klagebehandling = behandlingSomSkalDistribueres.oppdaterInnstillingsbrevDistribusjon(
                            distribusjonId = distribusjonId,
                            tidspunkt = nå(clock),
                        ),
                    )
                    log.info { "Innstillingsbrev markert som distribuert. distribusjonId: $distribusjonId, $kontekstTilLog" }
                }.onLeft {
                    log.error(it) { "Feil ved distribuering av innstillingsbrev. $kontekstTilLog" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av innstillingsbrev." }
        }
    }
}
