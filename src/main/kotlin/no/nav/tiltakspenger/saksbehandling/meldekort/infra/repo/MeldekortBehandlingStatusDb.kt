package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus

/**
 * @see MeldekortBehandlingStatus
 */

private enum class MeldekortBehandlingStatusDb {
    // TODO post-mvp: Rename denne til IKKE_UTFYLT og lag et migreringsskript for det.
    KLAR_TIL_UTFYLLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun String.toMeldekortBehandlingStatus(): MeldekortBehandlingStatus =
    when (MeldekortBehandlingStatusDb.valueOf(this)) {
        MeldekortBehandlingStatusDb.KLAR_TIL_UTFYLLING -> MeldekortBehandlingStatus.IKKE_BEHANDLET
        MeldekortBehandlingStatusDb.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatusDb.GODKJENT -> MeldekortBehandlingStatus.GODKJENT
        MeldekortBehandlingStatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }

fun MeldekortBehandlingStatus.toDb(): String =
    when (this) {
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldekortBehandlingStatusDb.KLAR_TIL_UTFYLLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatusDb.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldekortBehandlingStatusDb.GODKJENT
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatusDb.IKKE_RETT_TIL_TILTAKSPENGER
    }.toString()
