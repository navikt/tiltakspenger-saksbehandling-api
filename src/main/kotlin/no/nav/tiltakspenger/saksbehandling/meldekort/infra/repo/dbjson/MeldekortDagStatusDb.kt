package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus

enum class MeldekortDagStatusDb {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,

    /** Brukes foreløpig kun av bruker. */
    IKKE_BESVART,

    /** Brukes kun av saksbehandler. Sammensvarer med 'ikke tiltaksdag'*/
    IKKE_TILTAKSDAG,

    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldekortDagStatus.toDb(): MeldekortDagStatusDb {
    return when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusDb.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDb.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDb.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldekortDagStatusDb.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatusDb.FRAVÆR_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortDagStatusDb.FRAVÆR_ANNET
        MeldekortDagStatus.IKKE_BESVART -> MeldekortDagStatusDb.IKKE_BESVART
        MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldekortDagStatusDb.IKKE_TILTAKSDAG
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatusDb.IKKE_RETT_TIL_TILTAKSPENGER
    }
}

fun MeldekortDagStatusDb.toMeldekortDagStatus(): MeldekortDagStatus {
    return when (this) {
        MeldekortDagStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatusDb.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatusDb.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        MeldekortDagStatusDb.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        MeldekortDagStatusDb.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
        MeldekortDagStatusDb.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
        MeldekortDagStatusDb.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
        MeldekortDagStatusDb.IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
        MeldekortDagStatusDb.IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
        MeldekortDagStatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }
}
