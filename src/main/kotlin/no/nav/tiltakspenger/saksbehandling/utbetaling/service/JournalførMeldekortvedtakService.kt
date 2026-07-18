package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlignBeregninger
import no.nav.tiltakspenger.saksbehandling.felles.ErrorEveryNLogger
import no.nav.tiltakspenger.saksbehandling.journalføring.loggFeil
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import java.time.Clock

/**
 * Har ansvar for å generere pdf og sende meldekortvedtak til journalføring.
 * Denne er kun ment og kalles fra en jobb.
 */
class JournalførMeldekortvedtakService(
    private val journalførMeldekortKlient: JournalførMeldekortKlient,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val genererVedtaksbrevForMeldekortKlient: GenererVedtaksbrevForMeldekortKlient,
    private val navIdentClient: NavIdentClient,
    private val sakRepo: SakRepo,
    private val clock: Clock,
    private val brukMeldekortvedtakBrevV2: Boolean,
) {
    private val log = KotlinLogging.logger { }
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    suspend fun journalfør() {
        Either.catch {
            meldekortvedtakRepo.hentDeSomSkalJournalføres().forEach { meldekortvedtak ->
                val correlationId = CorrelationId.generate()
                log.info {
                    "Journalfører meldekortvedtak. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}"
                }

                Either.catch {
                    val sak = sakRepo.hentForSakId(meldekortvedtak.sakId)!!
                    val sammenligning = { beregningEtter: MeldeperiodeBeregning ->
                        val beregningFør = sak.meldeperiodeBeregninger.hentForrigeBeregning(
                            beregningEtter.id,
                            beregningEtter.kjedeId,
                        ).getOrElse {
                            when (it) {
                                MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenTidligereBeregninger -> null

                                MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenBeregningerForKjede,
                                MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.BeregningFinnesIkke,
                                -> {
                                    // TODO abn: kanskje vi burde kaste exception her?
                                    log.error { "Fant ikke beregningen ${beregningEtter.id} på kjede ${beregningEtter.kjedeId} - Dette er sannsynligvis en feil!" }
                                    null
                                }
                            }
                        }
                        sammenlignBeregninger(beregningFør, beregningEtter)
                    }
                    val tiltak = sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(meldekortvedtak.rammevedtak)

                    require(tiltak.isNotEmpty()) {
                        "Forventet at et det skal finnes tiltaksdeltakelse for meldekortvedtaksperioden"
                    }

                    val hentSaksbehandlersNavn: suspend (String) -> String =
                        if (meldekortvedtak.erAutomatiskBehandlet) {
                            { "Automatisk behandlet" }
                        } else {
                            navIdentClient::hentNavnForNavIdentEllerKast
                        }

                    val (pdfOgJson, pdfOgJsonPdfgenrs) =
                        if (brukMeldekortvedtakBrevV2) {
                            genererVedtaksbrevForMeldekortKlient.genererMeldekortvedtakBrevV2(
                                meldekortvedtak,
                                tiltaksdeltakelser = tiltak,
                                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                                sammenligning = sammenligning,
                            )
                        } else {
                            genererVedtaksbrevForMeldekortKlient.genererMeldekortvedtakBrev(
                                meldekortvedtak,
                                tiltaksdeltakelser = tiltak,
                                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                                sammenligning = sammenligning,
                            )
                        }.getOrElse {
                            it.feil.loggFeil(log, "generering av vedtaksbrev for meldekortvedtak", "Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}")
                            return@forEach
                        }
                    log.info { "Pdf generert for meldekortvedtak. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}" }
                    val journalpostId = journalførMeldekortKlient.journalførVedtaksbrevForMeldekortvedtak(
                        meldekortvedtak = meldekortvedtak,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    ).getOrElse {
                        it.loggFeil(log, "journalføring av meldekortvedtaksbrev", "Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}")
                        return@forEach
                    }.journalpostId
                    /*
                        TODO - pdfgenrs: fjern journalføringen av pdfgenrs-pdf'en når det er verifisert at pdf'en er ok.
                            Vi journalfører den kun for å manuelt kunne sjekke at pdfgenrs genererer riktig pdf i dev.
                            Feiler den, logger vi bare - dev-sammenligningen skal ikke stoppe journalføringsløpet.
                     */
                    pdfOgJsonPdfgenrs?.let {
                        journalførMeldekortKlient.journalførVedtaksbrevForMeldekortvedtak(
                            meldekortvedtak = meldekortvedtak,
                            pdfOgJson = it,
                            correlationId = correlationId,
                        ).onLeft { feil ->
                            feil.loggFeil(log, "journalføring av pdfgenrs-meldekortvedtaksbrev (kun dev-sammenligning)", "Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}")
                        }
                    }
                    log.info { "Meldekortvedtak journalført. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}. JournalpostId: $journalpostId" }
                    meldekortvedtakRepo.markerJournalført(meldekortvedtak.id, journalpostId, nå(clock))
                    log.info { "Meldekortvedtak markert som journalført. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}. JournalpostId: $journalpostId" }
                    errorEveryNLogger.reset()
                }.onLeft {
                    errorEveryNLogger.log(it) { "Ukjent feil skjedde under generering av brev og journalføring av meldekortvedtak. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av meldekortvedtak." }
        }
    }
}
