package no.nav.tiltakspenger.vedtak.felles

@JvmInline
value class OppgaveId(
    private val value: String,
) {
    override fun toString() = value
}
