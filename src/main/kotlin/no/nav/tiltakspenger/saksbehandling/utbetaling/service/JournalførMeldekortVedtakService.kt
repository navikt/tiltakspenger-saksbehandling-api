package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlign
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import java.time.Clock

/**
 * Har ansvar for å generere pdf og sende meldekortvedtak til journalføring.
 * Denne er kun ment og kalles fra en jobb.
 */
class JournalførMeldekortVedtakService(
    private val journalførMeldekortKlient: JournalførMeldekortKlient,
    private val meldekortVedtakRepo: MeldekortVedtakRepo,
    private val genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient,
    private val navIdentClient: NavIdentClient,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    suspend fun journalfør() {
        Either.catch {
            meldekortVedtakRepo.hentDeSomSkalJournalføres().forEach { meldekortVedtak ->
                val correlationId = CorrelationId.generate()
                log.info {
                    "Journalfører meldekortvedtak. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, meldekortvedtakId: ${meldekortVedtak.id}"
                }

                Either.catch {
                    val sak = sakRepo.hentForSakId(meldekortVedtak.sakId)!!
                    val sammenligning = { beregningEtter: MeldeperiodeBeregning ->
                        val beregningFør = sak.meldeperiodeBeregninger.sisteBeregningFør(
                            beregningEtter.id,
                            beregningEtter.kjedeId,
                        )
                        sammenlign(beregningFør, beregningEtter)
                    }
                    val tiltak = sak.vedtaksliste.hentTiltaksdataForPeriode(meldekortVedtak.beregningsperiode)

                    require(tiltak.isNotEmpty()) {
                        "Forventet at et det skal finnes tiltaksdeltagelse for meldekortvedtaksperioden"
                    }

                    // TODO: tilpass pdfgen-template for å ikke vise saksbehandler/beslutter ved automatisk behandling
                    val hentSaksbehandlersNavn: suspend (String) -> String =
                        if (meldekortVedtak.automatiskBehandlet) {
                            { "Automatisk behandlet" }
                        } else {
                            navIdentClient::hentNavnForNavIdent
                        }

                    val pdfOgJson =
                        genererVedtaksbrevForUtbetalingKlient.genererUtbetalingsvedtak(
                            meldekortVedtak,
                            sammenligning = sammenligning,
                            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                            tiltaksdeltagelser = tiltak,
                        ).getOrElse { return@forEach }
                    log.info { "Pdf generert for meldekortvedtak. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, meldekortvedtakId: ${meldekortVedtak.id}" }
                    val journalpostId = journalførMeldekortKlient.journalførMeldekortBehandling(
                        meldekortBehandling = sak.hentMeldekortBehandling(meldekortVedtak.meldekortId)!!,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "Meldekortvedtak journalført. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, meldekortvedtakId: ${meldekortVedtak.id}. JournalpostId: $journalpostId" }
                    meldekortVedtakRepo.markerJournalført(meldekortVedtak.id, journalpostId, nå(clock))
                    log.info { "Meldekortvedtak markert som journalført. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, meldekortvedtakId: ${meldekortVedtak.id}. JournalpostId: $journalpostId" }
                }.onLeft {
                    log.error(it) { "Ukjent feil skjedde under generering av brev og journalføring av meldekortvedtak. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, meldekortvedtakId: ${meldekortVedtak.id}" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under journalføring av meldekortvedtak." }
        }
    }
}
