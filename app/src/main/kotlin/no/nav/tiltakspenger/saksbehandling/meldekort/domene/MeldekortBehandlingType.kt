package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class MeldekortBehandlingType {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun String.tilMeldekortBehandlingType(): MeldekortBehandlingType = when (this) {
    "FØRSTE_BEHANDLING" -> MeldekortBehandlingType.FØRSTE_BEHANDLING
    "KORRIGERING" -> MeldekortBehandlingType.KORRIGERING
    else -> throw IllegalArgumentException("Ukjent meldekortbehanding type: $this")
}
