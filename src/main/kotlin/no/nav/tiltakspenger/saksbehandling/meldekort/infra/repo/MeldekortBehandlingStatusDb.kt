package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus

/**
 * @see MeldekortBehandlingStatus
 */

private enum class MeldekortBehandlingStatusDb {
    KLAR_TIL_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
    AUTOMATISK_BEHANDLET,
    AVBRUTT,
}

fun String.toMeldekortBehandlingStatus(): MeldekortBehandlingStatus =
    when (MeldekortBehandlingStatusDb.valueOf(this)) {
        MeldekortBehandlingStatusDb.KLAR_TIL_BEHANDLING -> MeldekortBehandlingStatus.UNDER_BEHANDLING
        MeldekortBehandlingStatusDb.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatusDb.UNDER_BESLUTNING -> MeldekortBehandlingStatus.UNDER_BESLUTNING
        MeldekortBehandlingStatusDb.GODKJENT -> MeldekortBehandlingStatus.GODKJENT
        MeldekortBehandlingStatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortBehandlingStatusDb.AUTOMATISK_BEHANDLET -> MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
        MeldekortBehandlingStatusDb.AVBRUTT -> MeldekortBehandlingStatus.AVBRUTT
    }

fun MeldekortBehandlingStatus.toDb(): String =
    when (this) {
        MeldekortBehandlingStatus.UNDER_BEHANDLING -> MeldekortBehandlingStatusDb.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatusDb.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.UNDER_BESLUTNING -> MeldekortBehandlingStatusDb.UNDER_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldekortBehandlingStatusDb.GODKJENT
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatusDb.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> MeldekortBehandlingStatusDb.AUTOMATISK_BEHANDLET
        MeldekortBehandlingStatus.AVBRUTT -> MeldekortBehandlingStatusDb.AVBRUTT
    }.toString()
