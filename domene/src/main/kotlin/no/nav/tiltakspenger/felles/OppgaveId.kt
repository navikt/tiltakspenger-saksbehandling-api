package no.nav.tiltakspenger.felles

@JvmInline
value class OppgaveId(
    private val value: String,
) {
    override fun toString() = value
}
