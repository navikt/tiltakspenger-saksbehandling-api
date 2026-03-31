package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingType

private enum class MeldekortbehandlingTypeDb {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun String.tilMeldekortbehandlingType(): MeldekortbehandlingType = when (MeldekortbehandlingTypeDb.valueOf(this)) {
    MeldekortbehandlingTypeDb.FØRSTE_BEHANDLING -> MeldekortbehandlingType.FØRSTE_BEHANDLING
    MeldekortbehandlingTypeDb.KORRIGERING -> MeldekortbehandlingType.KORRIGERING
}

fun MeldekortbehandlingType.tilDb() = when (this) {
    MeldekortbehandlingType.FØRSTE_BEHANDLING -> MeldekortbehandlingTypeDb.FØRSTE_BEHANDLING
    MeldekortbehandlingType.KORRIGERING -> MeldekortbehandlingTypeDb.KORRIGERING
}.toString()
