package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
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
                    // Et meldeperiode har ikke informasjon om tiltaksdeltagelsen, så vi må hente det fra rammevedtakene som gjelder for dette meldekortvedtaket.
                    // Det er mulig at flere rammevedtak gjelder for samme meldekortvedtak, f.eks. ved revurdering.
                    // Ved flere rammevedtak kan de inneholde de samme tiltaksdeltagelsene.
                    // Derfor må vi gruppere på eksternDeltagelseId og ta den nyeste.
                    val tiltak: Tiltaksdeltagelser = meldekortVedtak.rammevedtak
                        .map { sak.hentRammevedtakForId(it) }
                        .mapNotNull { vedtak -> vedtak.valgteTiltaksdeltakelser?.let { vedtak.opprettet to it } }
                        .flatMap { (opprettet, deltakelser) -> deltakelser.verdier.map { opprettet to it } }
                        .groupBy { it.second.eksternDeltagelseId }
                        .map { (_, verdi) -> verdi.maxBy { it.first }.second }
                        .let { Tiltaksdeltagelser(it) }

                    require(tiltak.isNotEmpty()) {
                        "Forventet at et det skal finnes tiltaksdeltagelse for meldekortvedtaksperioden"
                    }

                    val hentSaksbehandlersNavn: suspend (String) -> String =
                        if (meldekortVedtak.automatiskBehandlet) {
                            { "Automatisk behandlet" }
                        } else {
                            navIdentClient::hentNavnForNavIdent
                        }

                    val pdfOgJson =
                        genererVedtaksbrevForUtbetalingKlient.genererMeldekortVedtakBrev(
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
