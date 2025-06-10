package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo

class OppgaveMeldekortService(
    private val oppgaveKlient: OppgaveKlient,
    private val sakRepo: SakRepo,
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    private val log = KotlinLogging.logger {}

    // På sikt vil man bare opprette oppgave for meldekort som trenger en manuell gjennomgang, for meldekort skal generelt godkjennes maskinelt.
    suspend fun opprettOppgaveForMeldekortSomIkkeBehandlesAutomatisk() {
        Either.catch {
            log.debug { "Henter meldekort som det skal opprettes oppgaver for" }
            val meldekortList = brukersMeldekortRepo.hentMeldekortSomDetSkalOpprettesOppgaveFor()

            log.debug { "Fant ${meldekortList.size} meldekort som det skal opprettes oppgaver for" }
            meldekortList.forEach { meldekort ->
                val meldekortId = meldekort.id
                Either.catch {
                    val journalpostId = meldekort.journalpostId
                    val sak = sakRepo.hentForSakId(meldekort.sakId)
                        ?: log.warn { "Fant ikke sak for sakId ${meldekort.sakId}" }.let { return@forEach }

                    log.info { "Oppretter oppgave for meldekortId $meldekortId" }
                    val oppgaveId = oppgaveKlient.opprettOppgave(sak.fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT)

                    log.info { "Opprettet oppgave med id $oppgaveId for meldekort med id $meldekortId" }
                    brukersMeldekortRepo.oppdaterOppgaveId(meldekortId = meldekortId, oppgaveId = oppgaveId)
                }.onLeft {
                    log.error(it) { "Feil ved opprettelse av oppgave for meldekort $meldekortId" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver for meldekort" }
        }
    }
}
