package no.nav.tiltakspenger.vedtak.repository.meldekort

import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus

/**
 * @see MeldekortBehandlingStatus
 */

private enum class MeldekortstatusDb {
    // TODO post-mvp: Rename denne til IKKE_UTFYLT og lag et migreringsskript for det.
    KLAR_TIL_UTFYLLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun String.toMeldekortStatus(): MeldekortBehandlingStatus =
    when (MeldekortstatusDb.valueOf(this)) {
        MeldekortstatusDb.KLAR_TIL_UTFYLLING -> MeldekortBehandlingStatus.IKKE_BEHANDLET
        MeldekortstatusDb.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
        MeldekortstatusDb.GODKJENT -> MeldekortBehandlingStatus.GODKJENT
        MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }

fun MeldekortBehandlingStatus.toDb(): String =
    when (this) {
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldekortstatusDb.KLAR_TIL_UTFYLLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortstatusDb.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldekortstatusDb.GODKJENT
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortstatusDb.IKKE_RETT_TIL_TILTAKSPENGER
    }.toString()
