package no.nav.tiltakspenger.saksbehandling.journalnotat.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.ErrorEveryNLogger
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.loggFeil
import no.nav.tiltakspenger.saksbehandling.journalnotat.GenererJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.JournalførJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalføringsnotat
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat
import no.nav.tiltakspenger.saksbehandling.journalnotat.tilJournalnotat
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import java.time.Clock

/**
 * Journalfører interne notater (NOTAT i Joark) for vedtak der saksbehandler har valgt å journalføre notatet.
 * Notatet sendes ikke til bruker, og distribueres derfor ikke.
 * Ment å kalles fra en jobb via [journalførNotater], som tar både rammevedtak og meldekortvedtak.
 */
class JournalførJournalnotatService(
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val genererJournalnotatKlient: GenererJournalnotatKlient,
    private val journalførJournalnotatKlient: JournalførJournalnotatKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    suspend fun journalførNotater() {
        journalførNotaterForRammevedtak()
        journalførNotaterForMeldekortvedtak()
    }

    private suspend fun journalførNotaterForRammevedtak() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakIderMedNotatSomSkalJournalføres().forEach { vedtakId ->
                Either.catch {
                    val vedtak = rammevedtakRepo.hentForVedtakId(vedtakId) ?: return@forEach
                    journalfør(vedtak.tilJournalnotat(notatsdato = nå(clock).toLocalDate())) { journalpostId ->
                        rammevedtakRepo.markerNotatJournalført(vedtakId, Journalføringsnotat(journalpostId, nå(clock)))
                    }
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved journalføring av notat for rammevedtak $vedtakId" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av notater for rammevedtak." }
        }
    }

    private suspend fun journalførNotaterForMeldekortvedtak() {
        Either.catch {
            meldekortvedtakRepo.hentMeldekortvedtakIderMedNotatSomSkalJournalføres().forEach { vedtakId ->
                Either.catch {
                    val vedtak = meldekortvedtakRepo.hentForVedtakId(vedtakId) ?: return@forEach
                    journalfør(vedtak.tilJournalnotat(notatsdato = nå(clock).toLocalDate())) { journalpostId ->
                        meldekortvedtakRepo.markerNotatJournalført(vedtakId, Journalføringsnotat(journalpostId, nå(clock)))
                    }
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved journalføring av notat for meldekortvedtak $vedtakId" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av notater for meldekortvedtak." }
        }
    }

    private suspend fun journalfør(
        journalnotat: Journalnotat,
        markerJournalført: (JournalpostId) -> Unit,
    ) {
        val correlationId = CorrelationId.generate()
        val loggkontekst =
            "sakId: ${journalnotat.sakId}, saksnummer: ${journalnotat.saksnummer}, vedtakId: ${journalnotat.vedtakId}, vedtakstype: ${journalnotat.vedtakstype}"

        log.info { "Genererer journalnotat. $loggkontekst" }
        val pdfOgJson = genererJournalnotatKlient.genererJournalnotat(
            journalnotat = journalnotat,
            hentBrukersNavn = personService::hentNavn,
            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdentEllerKast,
        ).getOrElse {
            it.feil.loggFeil(log, "generering av journalnotat", loggkontekst)
            return
        }

        log.info { "Journalnotat generert, journalfører. $loggkontekst" }
        val journalpostId = journalførJournalnotatKlient.journalførJournalnotat(
            journalnotat = journalnotat,
            pdfOgJson = pdfOgJson,
            correlationId = correlationId,
        ).getOrElse {
            it.loggFeil(log, "journalføring av journalnotat", loggkontekst)
            return
        }.journalpostId

        markerJournalført(journalpostId)
        log.info { "Journalnotat journalført og markert som journalført. journalpostId: $journalpostId, $loggkontekst" }
        errorEveryNLogger.reset()
    }
}
