package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlign
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import java.time.Clock

/**
 * Har ansvar for å generere pdf og sende meldekortvedtak til journalføring.
 * Denne er kun ment og kalles fra en jobb.
 */
class JournalførMeldekortvedtakService(
    private val journalførMeldekortKlient: JournalførMeldekortKlient,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient,
    private val navIdentClient: NavIdentClient,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

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
                        sammenlign(beregningFør, beregningEtter)
                    }
                    val tiltak = sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(meldekortvedtak.rammevedtak)

                    require(tiltak.isNotEmpty()) {
                        "Forventet at et det skal finnes tiltaksdeltakelse for meldekortvedtaksperioden"
                    }

                    val hentSaksbehandlersNavn: suspend (String) -> String =
                        if (meldekortvedtak.automatiskBehandlet) {
                            { "Automatisk behandlet" }
                        } else {
                            navIdentClient::hentNavnForNavIdent
                        }

                    val pdfOgJson =
                        genererVedtaksbrevForUtbetalingKlient.genererMeldekortvedtakBrev(
                            meldekortvedtak,
                            tiltaksdeltakelser = tiltak,
                            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                            sammenligning = sammenligning,
                            false,
                        ).getOrElse { return@forEach }
                    log.info { "Pdf generert for meldekortvedtak. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}" }
                    val journalpostId = journalførMeldekortKlient.journalførMeldekortvedtak(
                        meldekortvedtak = meldekortvedtak,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "Meldekortvedtak journalført. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}. JournalpostId: $journalpostId" }
                    meldekortvedtakRepo.markerJournalført(meldekortvedtak.id, journalpostId, nå(clock))
                    log.info { "Meldekortvedtak markert som journalført. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}. JournalpostId: $journalpostId" }
                }.onLeft {
                    log.error(it) { "Ukjent feil skjedde under generering av brev og journalføring av meldekortvedtak. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}, meldekortvedtakId: ${meldekortvedtak.id}" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under journalføring av meldekortvedtak." }
        }
    }
}
