package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Miljøverdi
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.Personhendelse
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.harInnvilgetTiltakspengerEtterDato
import no.nav.tiltakspenger.saksbehandling.vedtak.harInnvilgetTiltakspengerPåDato
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class PersonhendelseJobb(
    private val personhendelseRepo: PersonhendelseRepo,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForPersonhendelser() {
        val personhendelseIder = personhendelseRepo.hentIderUtenOppgave()
        personhendelseIder.forEach { id ->
            try {
                opprettOppgaveForPersonhendelse(id)
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved behandling av personhendelse med id $id" }
            }
        }
    }

    suspend fun opprettOppgaveForPersonhendelse(personhendelseId: UUID) {
        val personhendelse = personhendelseRepo.hent(personhendelseId) ?: return
        val sak = sakRepo.hentForSakId(personhendelse.sakId)!!
        if ((!personhendelse.gjelderAdressebeskyttelse() && mottarTiltakspengerNaEllerIFremtiden(sak)) ||
            (personhendelse.gjelderAdressebeskyttelse() && sak.behandlinger.harEnEllerFlereÅpneBehandlinger)
        ) {
            val oppgavebehov = personhendelse.finnOppgavebehov()

            log.info { "Oppretter oppgave for hendelse med id ${personhendelse.hendelseId}" }
            val oppgaveId = oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                fnr = sak.fnr,
                oppgavebehov = oppgavebehov,
            ).getOrElse { feil ->
                feil.loggFeil(log, "opprettelse av gosysoppgave for personhendelse", "hendelseId: ${personhendelse.hendelseId}")
                return
            }
            personhendelseRepo.lagreOppgaveId(personhendelse.id, oppgaveId)
            log.info { "Lagret oppgaveId $oppgaveId for personhendelse med hendelsesId ${personhendelse.hendelseId}" }
        } else {
            personhendelseRepo.slett(personhendelse.id)
            log.info { "Skal ikke opprette oppgave, slettet personhendelse med hendelsesId ${personhendelse.hendelseId}" }
        }
    }

    suspend fun opprydning() {
        val personhendelseIder = personhendelseRepo.hentIderMedOppgave(nå(clock).minusHours(1))
        personhendelseIder.forEach { id ->
            try {
                ryddOppPersonhendelse(id)
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved opprydning av personhendelse med id $id" }
            }
        }
    }

    suspend fun ryddOppPersonhendelse(personhendelseId: UUID) {
        val personhendelse = personhendelseRepo.hentMedOppgaveId(personhendelseId) ?: return
        val hendelseId = personhendelse.hendelseId
        val oppgaveId = personhendelse.oppgaveId

        val ferdigstilt = oppgaveKlient.erFerdigstilt(oppgaveId).getOrElse { feil ->
            feil.loggFeil(log, "sjekk av om gosysoppgave er ferdigstilt", "oppgaveId: $oppgaveId, hendelseId: $hendelseId")
            return
        }
        if (ferdigstilt) {
            log.info { "Oppgave med id $oppgaveId er ferdigstilt, sletter innslag for personhendelse med hendelseId $hendelseId" }
            personhendelseRepo.slett(personhendelseId)
        } else {
            log.info { "Oppgave med id $oppgaveId er ikke ferdigstilt, oppdaterer sist sjekket for personhendelse med hendelseId $hendelseId" }
            personhendelseRepo.oppdaterOppgaveSistSjekket(personhendelseId)
        }
    }

    private fun mottarTiltakspengerNaEllerIFremtiden(
        sak: Sak,
        dato: LocalDate = LocalDate.now(clock),
    ): Boolean = sak.harInnvilgetTiltakspengerPåDato(dato) || sak.harInnvilgetTiltakspengerEtterDato(dato)

    private fun Personhendelse.finnOppgavebehov(): Oppgavebehov =
        when (opplysningstype) {
            Opplysningstype.DOEDSFALL_V1 -> Oppgavebehov.DOED
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Oppgavebehov.ADRESSEBESKYTTELSE
        }
}

fun (() -> PersonhendelseJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-personhendelse-opprett-opgave",
            intervall = Miljøverdi.lik(1.minutes),
            initialDelay = Miljøverdi.lik(1.minutes),
            utfør = { _ ->
                this().opprettOppgaveForPersonhendelser()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "saksbehandling-jobb-personhendelse-opprydning",
            intervall = Miljøverdi.lik(1.minutes),
            initialDelay = Miljøverdi.lik(1.minutes),
            utfør = { _ ->
                this().opprydning()
                TaskResultat.Ferdig
            },
        ),
    )
}
