package no.nav.tiltakspenger.vedtak.repository.meldekort

import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus

/**
 * @see MeldekortStatus
 */

private enum class MeldekortstatusDb {
    // TODO post-mvp: Rename denne til IKKE_UTFYLT og lag et migreringsskript for det.
    KLAR_TIL_UTFYLLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun String.toMeldekortStatus(): MeldekortStatus =
    when (MeldekortstatusDb.valueOf(this)) {
        MeldekortstatusDb.KLAR_TIL_UTFYLLING -> MeldekortStatus.IKKE_UTFYLT
        MeldekortstatusDb.KLAR_TIL_BESLUTNING -> MeldekortStatus.KLAR_TIL_BESLUTNING
        MeldekortstatusDb.GODKJENT -> MeldekortStatus.GODKJENT
        MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }

fun MeldekortStatus.toDb(): String =
    when (this) {
        MeldekortStatus.IKKE_UTFYLT -> MeldekortstatusDb.KLAR_TIL_UTFYLLING
        MeldekortStatus.KLAR_TIL_BESLUTNING -> MeldekortstatusDb.KLAR_TIL_BESLUTNING
        MeldekortStatus.GODKJENT -> MeldekortstatusDb.GODKJENT
        MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER
    }.toString()
