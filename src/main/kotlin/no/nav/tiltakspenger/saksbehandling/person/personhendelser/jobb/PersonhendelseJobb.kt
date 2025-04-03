package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

class PersonhendelseJobb(
    private val personhendelseRepository: PersonhendelseRepository,
    private val sakRepo: SakRepo,
    private val oppgaveGateway: OppgaveGateway,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForPersonhendelser() {
        val personhendelser = personhendelseRepository.hentAlleUtenOppgave()
        personhendelser.forEach { personhendelse ->
            try {
                val sakId = personhendelse.sakId
                val sak = sakRepo.hentForSakId(sakId)
                if (sak == null) {
                    log.error { "Fant ikke sak for sakId $sakId, skal ikke kunne skje" }
                    throw IllegalStateException("Fant ikke sak for sakId $sakId")
                }
                if (mottarTiltakspengerNaEllerIFremtiden(sak)) {
                    log.info { "Oppretter oppgave for hendelse med id ${personhendelse.hendelseId}" }
                    val oppgaveId = oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(
                        fnr = sak.fnr,
                        oppgavebehov = personhendelse.finnOppgavebehov(),
                    )
                    personhendelseRepository.lagreOppgaveId(personhendelse.id, oppgaveId)
                    log.info { "Lagret oppgaveId $oppgaveId for personhendelse med hendelsesId ${personhendelse.hendelseId}" }
                } else {
                    personhendelseRepository.slett(personhendelse.id)
                    log.info { "Bruker mottar ikke tiltakspenger, slettet personhendelse med hendelsesId ${personhendelse.hendelseId}" }
                }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved behandling av personhendelse med id ${personhendelse.id}" }
            }
        }
    }

    suspend fun opprydning() {
        val hendelserMedOppgave = personhendelseRepository.hentAlleMedOppgave()
        hendelserMedOppgave.forEach {
            try {
                val hendelseId = it.hendelseId
                val oppgaveId = it.oppgaveId

                if (oppgaveId != null) {
                    val ferdigstilt = oppgaveGateway.erFerdigstilt(oppgaveId)
                    if (ferdigstilt) {
                        log.info { "Oppgave med id $oppgaveId er ferdigstilt, sletter innslag for personhendelse med hendelseId $hendelseId" }
                        personhendelseRepository.slett(it.id)
                    } else {
                        log.info { "Oppgave med id $oppgaveId er ikke ferdigstilt, oppdaterer sist sjekket for personhendelse med hendelseId $hendelseId" }
                        personhendelseRepository.oppdaterOppgaveSistSjekket(it.id)
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved opprydning av personhendelse med id ${it.id}" }
            }
        }
    }

    private fun mottarTiltakspengerNaEllerIFremtiden(
        sak: Sak,
        dato: LocalDate = LocalDate.now(),
    ): Boolean =
        sak.vedtaksliste.harInnvilgetTiltakspengerPaDato(dato) ||
            sak.vedtaksliste.harInnvilgetTiltakspengerEtterDato(dato)

    private fun PersonhendelseDb.finnOppgavebehov() =
        when (opplysningstype) {
            Opplysningstype.FORELDERBARNRELASJON_V1 -> Oppgavebehov.FATT_BARN
            Opplysningstype.DOEDSFALL_V1 -> Oppgavebehov.DOED
            else -> throw IllegalArgumentException("Skal ikke opprette oppgave for opplysningstype $opplysningstype")
        }
}
