package no.nav.tiltakspenger.saksbehandling.journalføring

@JvmInline
value class JournalpostId(
    private val value: String,
) {
    override fun toString() = value
}
