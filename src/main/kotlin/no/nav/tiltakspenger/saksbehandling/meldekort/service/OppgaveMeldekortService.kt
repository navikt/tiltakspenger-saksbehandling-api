package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo

class OppgaveMeldekortService(
    private val oppgaveGateway: OppgaveGateway,
    private val sakRepo: SakRepo,
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    private val log = KotlinLogging.logger {}

    // På sikt vil man bare opprette oppgave for meldekort som trenger en manuell gjennomgang, for meldekort skal generelt godkjennes maskinelt.
    suspend fun opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk() {
        Either.catch {
            log.debug { "Henter meldekort som det skal opprettes oppgaver for" }
            val meldekortList = brukersMeldekortRepo.hentMeldekortSomIkkeSkalGodkjennesAutomatisk()

            log.debug { "Fant ${meldekortList.size} meldekort som det skal opprettes oppgaver for" }
            meldekortList.forEach { meldekort ->
                Either.catch {
                    val journalpostId = meldekort.journalpostId
                    val sak = sakRepo.hentForSakId(meldekort.sakId)
                        ?: log.warn { "Fant ikke sak for sakId ${meldekort.sakId}" }.let { return@forEach }

                    log.info { "Oppretter oppgave for meldekortId ${meldekort.id}" }
                    val oppgaveId = oppgaveGateway.opprettOppgave(sak.fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT)

                    log.info { "Opprettet oppgave med id $oppgaveId for meldekort med id ${meldekort.id}" }
                    brukersMeldekortRepo.oppdater(meldekort.copy(oppgaveId = oppgaveId))
                }.onLeft {
                    log.error(it) { "Feil ved opprettelse av oppgave for meldekort ${meldekort.id}" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver for meldekort" }
        }
    }
}
