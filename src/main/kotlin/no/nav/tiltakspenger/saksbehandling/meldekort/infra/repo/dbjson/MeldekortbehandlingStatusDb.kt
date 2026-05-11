package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

/**
 * @see MeldekortbehandlingStatus
 */

private enum class MeldekortbehandlingStatusDb {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    AVBRUTT,
}

fun String.toMeldekortbehandlingStatus(): MeldekortbehandlingStatus =
    when (MeldekortbehandlingStatusDb.valueOf(this)) {
        MeldekortbehandlingStatusDb.KLAR_TIL_BEHANDLING -> MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
        MeldekortbehandlingStatusDb.UNDER_BEHANDLING -> MeldekortbehandlingStatus.UNDER_BEHANDLING
        MeldekortbehandlingStatusDb.KLAR_TIL_BESLUTNING -> MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
        MeldekortbehandlingStatusDb.UNDER_BESLUTNING -> MeldekortbehandlingStatus.UNDER_BESLUTNING
        MeldekortbehandlingStatusDb.GODKJENT -> MeldekortbehandlingStatus.GODKJENT
        MeldekortbehandlingStatusDb.AUTOMATISK_BEHANDLET -> MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET
        MeldekortbehandlingStatusDb.AVBRUTT -> MeldekortbehandlingStatus.AVBRUTT
    }

fun MeldekortbehandlingStatus.toDb(): String =
    when (this) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> MeldekortbehandlingStatusDb.KLAR_TIL_BEHANDLING
        MeldekortbehandlingStatus.UNDER_BEHANDLING -> MeldekortbehandlingStatusDb.UNDER_BEHANDLING
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortbehandlingStatusDb.KLAR_TIL_BESLUTNING
        MeldekortbehandlingStatus.UNDER_BESLUTNING -> MeldekortbehandlingStatusDb.UNDER_BESLUTNING
        MeldekortbehandlingStatus.GODKJENT -> MeldekortbehandlingStatusDb.GODKJENT
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> MeldekortbehandlingStatusDb.AUTOMATISK_BEHANDLET
        MeldekortbehandlingStatus.AVBRUTT -> MeldekortbehandlingStatusDb.AVBRUTT
    }.toString()
