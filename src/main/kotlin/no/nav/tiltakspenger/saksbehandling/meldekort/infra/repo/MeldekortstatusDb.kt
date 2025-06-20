package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus

enum class MeldekortstatusDb {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,

    /** Brukes foreløpig kun av bruker. */
    IKKE_BESVART,

    /** Brukes kun av saksbehandler. Sammensvarer med 'ikke tiltaksdag'*/
    IKKE_TILTAKSDAG,

    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldekortDagStatus.toDb(): MeldekortstatusDb {
    return when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortstatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortstatusDb.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortstatusDb.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortstatusDb.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortstatusDb.FRAVÆR_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortstatusDb.FRAVÆR_ANNET
        MeldekortDagStatus.IKKE_BESVART -> MeldekortstatusDb.IKKE_BESVART
        MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldekortstatusDb.IKKE_TILTAKSDAG
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER
    }
}

fun MeldekortstatusDb.toMeldekortDagStatus(): MeldekortDagStatus {
    return when (this) {
        MeldekortstatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortstatusDb.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortstatusDb.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        MeldekortstatusDb.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        MeldekortstatusDb.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
        MeldekortstatusDb.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
        MeldekortstatusDb.IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
        MeldekortstatusDb.IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
        MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }
}
