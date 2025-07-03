package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.sammenlign
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.time.Clock

/**
 * Har ansvar for å generere pdf og sende utbetalingsvedtak til journalføring.
 * Denne er kun ment og kalles fra en jobb.
 */
class JournalførUtbetalingsvedtakService(
    private val journalførMeldekortKlient: JournalførMeldekortKlient,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient,
    private val navIdentClient: NavIdentClient,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    suspend fun journalfør() {
        Either.catch {
            utbetalingsvedtakRepo.hentDeSomSkalJournalføres().forEach { utbetalingsvedtak ->
                require(utbetalingsvedtak.beregningKilde is MeldeperiodeBeregning.FraMeldekort) {
                    "Støtter ikke journalføring av utbetalingsvedtak fra revurdering ennå!"
                }

                val correlationId = CorrelationId.generate()
                log.info { "Journalfører utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                Either.catch {
                    val sak = sakRepo.hentForSakId(utbetalingsvedtak.sakId)!!
                    val sammenligning = { beregningEtter: MeldeperiodeBeregning ->
                        val beregningFør = sak.meldeperiodeBeregninger.sisteBeregningFør(
                            beregningEtter.id,
                            beregningEtter.kjedeId,
                        )
                        sammenlign(beregningFør, beregningEtter)
                    }
                    val tiltak = sak.vedtaksliste.hentTiltaksdataForPeriode(utbetalingsvedtak.periode)

                    require(tiltak.isNotEmpty()) {
                        "Forventet at et det skal finnes tiltaksdeltagelse for utbetalingsvedtaksperioden"
                    }

                    // TODO: tilpass pdfgen-template for å ikke vise saksbehandler/beslutter ved automatisk behandling
                    val hentSaksbehandlersNavn: suspend (String) -> String =
                        if (utbetalingsvedtak.automatiskBehandlet) {
                            { "Automatisk behandlet" }
                        } else {
                            navIdentClient::hentNavnForNavIdent
                        }

                    val pdfOgJson =
                        genererVedtaksbrevForUtbetalingKlient.genererUtbetalingsvedtak(
                            utbetalingsvedtak,
                            sammenligning = sammenligning,
                            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                            tiltaksdeltagelser = tiltak,
                        ).getOrElse { return@forEach }
                    log.info { "Pdf generert for utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                    val journalpostId = journalførMeldekortKlient.journalførMeldekortBehandling(
                        meldekortBehandling = sak.hentMeldekortBehandling(utbetalingsvedtak.beregningKilde.id)!!,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "utbetalingsvedtak journalført. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}. JournalpostId: $journalpostId" }
                    utbetalingsvedtakRepo.markerJournalført(utbetalingsvedtak.id, journalpostId, nå(clock))
                    log.info { "Utbetalingsvedtak markert som journalført. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}. JournalpostId: $journalpostId" }
                }.onLeft {
                    log.error(it) { "Ukjent feil skjedde under generering av brev og journalføring av utbetalingsvedtak. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under journalføring av utbetalingsvedtak." }
        }
    }
}
