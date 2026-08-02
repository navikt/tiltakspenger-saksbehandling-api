package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.harInnvilgetTiltakspengerEtterDato
import no.nav.tiltakspenger.saksbehandling.vedtak.harInnvilgetTiltakspengerPåDato
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class PersonhendelseJobb(
    private val personhendelseRepository: PersonhendelseRepository,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForPersonhendelser() {
        val personhendelseIder = personhendelseRepository.hentIderUtenOppgave()
        personhendelseIder.forEach { id ->
            try {
                opprettOppgaveForPersonhendelse(id)
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved behandling av personhendelse med id $id" }
            }
        }
    }

    suspend fun opprettOppgaveForPersonhendelse(personhendelseId: UUID) {
        val personhendelse = personhendelseRepository.hent(personhendelseId) ?: return
        val sak = sakRepo.hentForSakId(personhendelse.sakId)!!
        if ((!personhendelse.gjelderAdressebeskyttelse() && mottarTiltakspengerNaEllerIFremtiden(sak)) ||
            (personhendelse.gjelderAdressebeskyttelse() && sak.behandlinger.harEnEllerFlereÅpneBehandlinger)
        ) {
            val oppgavebehov = personhendelse.finnOppgavebehov() ?: return

            log.info { "Oppretter oppgave for hendelse med id ${personhendelse.hendelseId}" }
            val oppgaveId = oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                fnr = sak.fnr,
                oppgavebehov = oppgavebehov,
            ).getOrElse { feil ->
                feil.loggFeil(log, "opprettelse av gosysoppgave for personhendelse", "hendelseId: ${personhendelse.hendelseId}")
                return
            }
            personhendelseRepository.lagreOppgaveId(personhendelse.id, oppgaveId)
            log.info { "Lagret oppgaveId $oppgaveId for personhendelse med hendelsesId ${personhendelse.hendelseId}" }
        } else {
            personhendelseRepository.slett(personhendelse.id)
            log.info { "Skal ikke opprette oppgave, slettet personhendelse med hendelsesId ${personhendelse.hendelseId}" }
        }
    }

    suspend fun opprydning() {
        val personhendelseIder = personhendelseRepository.hentIderMedOppgave()
        personhendelseIder.forEach { id ->
            try {
                ryddOppPersonhendelse(id)
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved opprydning av personhendelse med id $id" }
            }
        }
    }

    suspend fun ryddOppPersonhendelse(personhendelseId: UUID) {
        val personhendelse = personhendelseRepository.hent(personhendelseId) ?: return
        val hendelseId = personhendelse.hendelseId
        val oppgaveId = personhendelse.oppgaveId ?: return

        val ferdigstilt = oppgaveKlient.erFerdigstilt(oppgaveId).getOrElse { feil ->
            feil.loggFeil(log, "sjekk av om gosysoppgave er ferdigstilt", "oppgaveId: $oppgaveId, hendelseId: $hendelseId")
            return
        }
        if (ferdigstilt) {
            log.info { "Oppgave med id $oppgaveId er ferdigstilt, sletter innslag for personhendelse med hendelseId $hendelseId" }
            personhendelseRepository.slett(personhendelse.id)
        } else {
            log.info { "Oppgave med id $oppgaveId er ikke ferdigstilt, oppdaterer sist sjekket for personhendelse med hendelseId $hendelseId" }
            personhendelseRepository.oppdaterOppgaveSistSjekket(personhendelse.id)
        }
    }

    private fun mottarTiltakspengerNaEllerIFremtiden(
        sak: Sak,
        dato: LocalDate = LocalDate.now(clock),
    ): Boolean = sak.harInnvilgetTiltakspengerPåDato(dato) || sak.harInnvilgetTiltakspengerEtterDato(dato)

    private fun PersonhendelseDb.finnOppgavebehov(): Oppgavebehov? =
        when (opplysningstype) {
            Opplysningstype.DOEDSFALL_V1 -> Oppgavebehov.DOED
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Oppgavebehov.ADRESSEBESKYTTELSE
        }
}
