package no.nav.tiltakspenger.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.NavIdentClient
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.meldekort.ports.JournalførMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo

/**
 * Har ansvar for å generere pdf og sende utbetalingsvedtak til journalføring.
 * Denne er kun ment og kalles fra en jobb.
 */
class JournalførUtbetalingsvedtakService(
    private val journalførMeldekortGateway: JournalførMeldekortGateway,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val genererUtbetalingsvedtakGateway: GenererUtbetalingsvedtakGateway,
    private val navIdentClient: NavIdentClient,
    private val sakRepo: SakRepo,
) {
    private val log = KotlinLogging.logger { }

    suspend fun journalfør() {
        Either.catch {
            utbetalingsvedtakRepo.hentDeSomSkalJournalføres().forEach { utbetalingsvedtak ->
                val correlationId = CorrelationId.generate()
                log.info { "Journalfører utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                Either.catch {
                    val sak = sakRepo.hentForSakId(utbetalingsvedtak.sakId)!!
                    val tiltak = sak.vedtaksliste.hentTiltaksdataForPeriode(utbetalingsvedtak.periode)!!
                    val pdfOgJson =
                        genererUtbetalingsvedtakGateway.genererUtbetalingsvedtak(
                            utbetalingsvedtak,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                            tiltaksnavn = tiltak.tiltaksnavn,
                            eksternDeltagelseId = tiltak.eksternDeltagelseId,
                            eksternGjennomføringId = tiltak.eksternGjennomføringId,
                        ).getOrElse { return@forEach }
                    log.info { "Pdf generert for utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                    val journalpostId = journalførMeldekortGateway.journalførMeldekort(
                        meldekort = utbetalingsvedtak.meldekort,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "utbetalingsvedtak journalført. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}. JournalpostId: $journalpostId" }
                    utbetalingsvedtakRepo.markerJournalført(utbetalingsvedtak.id, journalpostId, nå())
                    log.info { "Utbetalingsvedtak markert som journalført. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}. JournalpostId: $journalpostId" }
                }.onLeft {
                    log.error(it) { "Ukjent feil skjedde under generering av brev og journalføring av utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                }
            }
        }.onLeft {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil skjedde under journalføring av utbetalingsvedtak." }
            sikkerlogg.error(it) { "Ukjent feil skjedde under journalføring av utbetalingsvedtak." }
        }
    }
}
