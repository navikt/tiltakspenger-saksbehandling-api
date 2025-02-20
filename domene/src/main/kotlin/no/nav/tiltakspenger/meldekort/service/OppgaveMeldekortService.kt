package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo

class OppgaveMeldekortService(
    private val oppgaveGateway: OppgaveGateway,
    private val sakRepo: SakRepo,
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    private val log = KotlinLogging.logger {}

    // På sikt vil man bare opprette oppgave for meldekort som trenger en manuell gjennomgang, for meldekort skal generelt godkjennes maskinelt.
    suspend fun opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk() {
        brukersMeldekortRepo.hentMeldekortSomIKkeSkalGodkjennesAutomatisk().forEach { meldekort ->
            val journalpostId = meldekort.journalpostId
                ?: log.warn { "Fant ikke journalpostId for meldekortId ${meldekort.id}" }.let { return@forEach }
            val sak = sakRepo.hentForSakId(meldekort.sakId)
                ?: log.warn { "Fant ikke sak for sakId ${meldekort.sakId}" }.let { return@forEach }
            oppgaveGateway.opprettOppgave(sak.fnr, JournalpostId(journalpostId), Oppgavebehov.NYTT_MELDEKORT)
        }
    }
}
