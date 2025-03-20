package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.felles.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo

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
                    val tiltak = sak.vedtaksliste.hentTiltaksdataForPeriode(utbetalingsvedtak.periode)
                    val pdfOgJson =
                        genererUtbetalingsvedtakGateway.genererUtbetalingsvedtak(
                            utbetalingsvedtak,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                            tiltaksdeltagelser = tiltak,
                        ).getOrElse { return@forEach }
                    log.info { "Pdf generert for utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                    val journalpostId = journalførMeldekortGateway.journalførMeldekortBehandling(
                        meldekortBehandling = utbetalingsvedtak.meldekortbehandling,
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
