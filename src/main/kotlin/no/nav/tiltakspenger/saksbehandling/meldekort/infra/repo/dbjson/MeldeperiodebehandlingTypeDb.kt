package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType

private enum class MeldeperiodebehandlingTypeDb {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun String.tilMeldeperiodebehandlingType(): MeldeperiodebehandlingType = when (MeldeperiodebehandlingTypeDb.valueOf(this)) {
    MeldeperiodebehandlingTypeDb.FØRSTE_BEHANDLING -> MeldeperiodebehandlingType.FØRSTE_BEHANDLING
    MeldeperiodebehandlingTypeDb.KORRIGERING -> MeldeperiodebehandlingType.KORRIGERING
}

fun MeldeperiodebehandlingType.tilDb() = when (this) {
    MeldeperiodebehandlingType.FØRSTE_BEHANDLING -> MeldeperiodebehandlingTypeDb.FØRSTE_BEHANDLING
    MeldeperiodebehandlingType.KORRIGERING -> MeldeperiodebehandlingTypeDb.KORRIGERING
}.toString()
