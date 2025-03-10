package no.nav.tiltakspenger.vedtak.felles.journalføring

@JvmInline
value class JournalpostId(
    private val value: String,
) {
    override fun toString() = value
}
