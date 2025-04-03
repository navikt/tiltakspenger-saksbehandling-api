package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import java.time.LocalDate

data class MeldekortDag(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
)

enum class MeldekortDagStatus {
    SPERRET,
    IKKE_UTFYLT,
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    IKKE_DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
}
