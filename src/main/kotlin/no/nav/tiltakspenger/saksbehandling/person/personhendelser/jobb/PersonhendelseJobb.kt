package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
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
    private val oppgaveKlient: OppgaveKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForPersonhendelser() {
        val personhendelser = personhendelseRepository.hentAlleUtenOppgave()
        personhendelser.forEach { personhendelse ->
            try {
                val sakId = personhendelse.sakId
                val sak = sakRepo.hentForSakId(sakId)!!
                if ((!personhendelse.gjelderAdressebeskyttelse() && mottarTiltakspengerNaEllerIFremtiden(sak)) ||
                    (personhendelse.gjelderAdressebeskyttelse() && harApenBehandling(sak))
                ) {
                    log.info { "Oppretter oppgave for hendelse med id ${personhendelse.hendelseId}" }
                    val oppgaveId = oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                        fnr = sak.fnr,
                        oppgavebehov = personhendelse.finnOppgavebehov(),
                    )
                    personhendelseRepository.lagreOppgaveId(personhendelse.id, oppgaveId)
                    log.info { "Lagret oppgaveId $oppgaveId for personhendelse med hendelsesId ${personhendelse.hendelseId}" }
                } else {
                    personhendelseRepository.slett(personhendelse.id)
                    log.info { "Skal ikke opprette oppgave, slettet personhendelse med hendelsesId ${personhendelse.hendelseId}" }
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
                    val ferdigstilt = oppgaveKlient.erFerdigstilt(oppgaveId)
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

    private fun harApenBehandling(sak: Sak): Boolean =
        sak.behandlinger.hentÅpneBehandlinger().isNotEmpty()

    private fun PersonhendelseDb.finnOppgavebehov() =
        when (opplysningstype) {
            Opplysningstype.FORELDERBARNRELASJON_V1 -> Oppgavebehov.FATT_BARN
            Opplysningstype.DOEDSFALL_V1 -> Oppgavebehov.DOED
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Oppgavebehov.ADRESSEBESKYTTELSE
        }
}
