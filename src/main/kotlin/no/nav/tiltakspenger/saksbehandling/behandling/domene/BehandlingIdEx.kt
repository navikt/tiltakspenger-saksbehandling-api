package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId

inline fun <T> BehandlingId.match(
    rammebehandlingId: (RammebehandlingId) -> T,
    meldekortId: (MeldekortId) -> T,
): T = when (this) {
    is RammebehandlingId -> rammebehandlingId(this)
    is MeldekortId -> meldekortId(this)
    else -> throw IllegalStateException("Ukjent BehandlingId-type: ${this::class}")
}
