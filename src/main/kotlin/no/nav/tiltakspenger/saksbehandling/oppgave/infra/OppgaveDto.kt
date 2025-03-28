package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.DayOfWeek
import java.time.LocalDate

const val TEMA_TILTAKSPENGER: String = "IND"

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
            oppgavetype = OppgaveType.OPPGAVETYPE_BEHANDLE_SAK.value,
        )

        fun opprettOppgaveRequestForEndretTiltaksdeltaker(
            fnr: Fnr,
        ) = OpprettOppgaveRequest(
            personident = fnr.verdi,
            journalpostId = null,
            beskrivelse = "Det har skjedd en endring i tiltaksdeltakelsen som kan påvirke tiltakspengeytelsen.",
            behandlesAvApplikasjon = null,
            oppgavetype = OppgaveType.OPPGAVETYPE_VURDER_KONSEKVENS_FOR_YTELSE.value,
        )

        fun opprettOppgaveRequestForMeldekort(
            fnr: Fnr,
            journalpostId: JournalpostId,
        ) = OpprettOppgaveRequest(
            personident = fnr.verdi,
            journalpostId = journalpostId.toString(),
            beskrivelse = "Nytt meldekort for tiltakspenger. Behandles i ny løsning.",
            behandlesAvApplikasjon = BEHANDLES_AV_APPLIKASJON,
            oppgavetype = OppgaveType.OPPGAVETYPE_VURDER_HENVENDELSE.value,
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

enum class OppgaveType(val value: String) {
    OPPGAVETYPE_BEHANDLE_SAK("BEH_SAK"),
    OPPGAVETYPE_VURDER_KONSEKVENS_FOR_YTELSE("VUR_KONS_YTE"),
    OPPGAVETYPE_VURDER_HENVENDELSE("VURD_HENV"),
}
