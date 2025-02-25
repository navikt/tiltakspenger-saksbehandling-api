package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
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
        log.info { "Henter meldekort som det skal opprettes oppgaver for" }
        val meldekortList = brukersMeldekortRepo.hentMeldekortSomIkkeSkalGodkjennesAutomatisk()

        log.info { "Fant ${meldekortList.size} meldekort som det skal opprettes oppgaver for" }
        meldekortList.forEach { meldekort ->
            val journalpostId = meldekort.journalpostId
                ?: log.warn { "Fant ikke journalpostId for meldekortId ${meldekort.id}" }.let { return@forEach }
            val sak = sakRepo.hentForSakId(meldekort.sakId)
                ?: log.warn { "Fant ikke sak for sakId ${meldekort.sakId}" }.let { return@forEach }

            log.info { "Oppretter oppgave for meldekortId ${meldekort.id}" }
            val oppgaveId = oppgaveGateway.opprettOppgave(sak.fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT)

            log.info { "Opprettet oppgave med id $oppgaveId for meldekort med id ${meldekort.id}" }
            brukersMeldekortRepo.oppdater(meldekort.copy(oppgaveId = oppgaveId))
        }
    }
}
