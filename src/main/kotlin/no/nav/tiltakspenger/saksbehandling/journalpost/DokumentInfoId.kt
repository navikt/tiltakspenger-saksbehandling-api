package no.nav.tiltakspenger.saksbehandling.journalpost

@JvmInline
value class DokumentInfoId(
    private val value: String,
) {
    override fun toString() = value
}
