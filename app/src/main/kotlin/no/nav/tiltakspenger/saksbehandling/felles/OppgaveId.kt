package no.nav.tiltakspenger.saksbehandling.felles

@JvmInline
value class OppgaveId(
    private val value: String,
) {
    override fun toString() = value
}
