package no.nav.tiltakspenger.saksbehandling.felles.journalføring

@JvmInline
value class JournalpostId(
    private val value: String,
) {
    override fun toString() = value
}
