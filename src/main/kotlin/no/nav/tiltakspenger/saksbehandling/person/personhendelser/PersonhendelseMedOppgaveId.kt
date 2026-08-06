package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

/**
 * Utsnittet av en [Personhendelse] som opprydningen trenger: hvilken oppgave hendelsen fikk, og hendelsens id hos PDL.
 */
data class PersonhendelseMedOppgaveId(
    val hendelseId: String,
    val oppgaveId: OppgaveId,
)
