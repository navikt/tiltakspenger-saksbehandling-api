package no.nav.tiltakspenger.felles.journalføring

@JvmInline
value class JournalpostId(
    private val value: String,
) {
    override fun toString() = value
}
