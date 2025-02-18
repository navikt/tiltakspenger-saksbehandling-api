package no.nav.tiltakspenger.vedtak.clients.oppgave

import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.Fnr
import java.time.DayOfWeek
import java.time.LocalDate

const val TEMA_TILTAKSPENGER: String = "IND"
const val OPPGAVETYPE_BEHANDLE_SAK: String = "BEH_SAK"
const val OPPGAVETYPE_VURDER_KONSEKVENS_FOR_YTELSE: String = "VUR_KONS_YTE"

const val BEHANDLES_AV_APPLIKASJON = "TILTAKSPENGER"

data class OpprettOppgaveRequest(
    val personident: String,
    val opprettetAvEnhetsnr: String = "9999",
    val journalpostId: String?,
    val behandlesAvApplikasjon: String?,
    val beskrivelse: String,
    val tema: String = TEMA_TILTAKSPENGER,
    val oppgavetype: String,
    val aktivDato: LocalDate = LocalDate.now(),
    val fristFerdigstillelse: LocalDate = finnFristForFerdigstillingAvOppgave(LocalDate.now().plusDays(3)),
    val prioritet: PrioritetType = PrioritetType.NORM,
) {
    companion object {
        fun opprettOppgaveRequestForSoknad(
            fnr: Fnr,
            journalpostId: JournalpostId,
        ) = OpprettOppgaveRequest(
            personident = fnr.verdi,
            journalpostId = journalpostId.toString(),
            beskrivelse = "Ny søknad om tiltakspenger. Behandles i ny løsning.",
            behandlesAvApplikasjon = BEHANDLES_AV_APPLIKASJON,
            oppgavetype = OPPGAVETYPE_BEHANDLE_SAK,
        )

        fun opprettOppgaveRequestForEndretTiltaksdeltaker(
            fnr: Fnr,
        ) = OpprettOppgaveRequest(
            personident = fnr.verdi,
            journalpostId = null,
            beskrivelse = "Det har skjedd en endring i tiltaksdeltakelsen som kan påvirke tiltakspengeytelsen.",
            behandlesAvApplikasjon = null,
            oppgavetype = OPPGAVETYPE_VURDER_KONSEKVENS_FOR_YTELSE,
        )
    }
}

fun finnFristForFerdigstillingAvOppgave(ferdigstillDato: LocalDate): LocalDate {
    return finnNesteArbeidsdag(ferdigstillDato)
}

fun finnNesteArbeidsdag(ferdigstillDato: LocalDate): LocalDate =
    when (ferdigstillDato.dayOfWeek) {
        DayOfWeek.SATURDAY -> ferdigstillDato.plusDays(2)
        DayOfWeek.SUNDAY -> ferdigstillDato.plusDays(1)
        else -> ferdigstillDato
    }

enum class PrioritetType {
    HOY,
    NORM,
    LAV,
}

data class OpprettOppgaveResponse(
    val id: Int,
)

data class FinnOppgaveResponse(
    val antallTreffTotalt: Int,
    val oppgaver: List<Oppgave>,
)

data class Oppgave(
    val id: Int,
    val status: OppgaveStatus,
    val versjon: Int,
) {
    fun erFerdigstilt() =
        status == OppgaveStatus.FERDIGSTILT || status == OppgaveStatus.FEILREGISTRERT
}

enum class OppgaveStatus {
    OPPRETTET,
    AAPNET,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
}

data class FerdigstillOppgaveRequest(
    val versjon: Int,
    val status: OppgaveStatus,
)
