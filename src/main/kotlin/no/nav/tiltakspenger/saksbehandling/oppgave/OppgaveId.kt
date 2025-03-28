package no.nav.tiltakspenger.saksbehandling.oppgave

@JvmInline
value class OppgaveId(
    private val value: String,
) {
    override fun toString() = value
}
