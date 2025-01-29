package no.nav.tiltakspenger.vedtak.clients.oppgave

import java.time.DayOfWeek
import java.time.LocalDate

const val TEMA_TILTAKSPENGER: String = "IND"
const val OPPGAVETYPE_BEHANDLE_SAK: String = "BEH_SAK"

data class OpprettOppgaveRequest(
    val personident: String,
    val opprettetAvEnhetsnr: String = "9999",
    val journalpostId: String,
    val behandlesAvApplikasjon: String = "TILTAKSPENGER",
    val beskrivelse: String = "Ny søknad om tiltakspenger. Behandles i ny løsning.",
    val tema: String = TEMA_TILTAKSPENGER,
    val oppgavetype: String = OPPGAVETYPE_BEHANDLE_SAK,
    val aktivDato: LocalDate = LocalDate.now(),
    val fristFerdigstillelse: LocalDate = finnFristForFerdigstillingAvOppgave(LocalDate.now().plusDays(3)),
    val prioritet: PrioritetType = PrioritetType.NORM,
)

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
