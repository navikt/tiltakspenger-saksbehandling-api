package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType

private enum class MeldekortBehandlingTypeDb {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun String.tilMeldekortBehandlingType(): MeldekortBehandlingType = when (MeldekortBehandlingTypeDb.valueOf(this)) {
    MeldekortBehandlingTypeDb.FØRSTE_BEHANDLING -> MeldekortBehandlingType.FØRSTE_BEHANDLING
    MeldekortBehandlingTypeDb.KORRIGERING -> MeldekortBehandlingType.KORRIGERING
}

fun MeldekortBehandlingType.tilDb() = when (this) {
    MeldekortBehandlingType.FØRSTE_BEHANDLING -> MeldekortBehandlingTypeDb.FØRSTE_BEHANDLING
    MeldekortBehandlingType.KORRIGERING -> MeldekortBehandlingTypeDb.KORRIGERING
}.toString()
