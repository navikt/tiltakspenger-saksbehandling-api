package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId

sealed interface KunneIkkeOppdatereHelgForMeldekort {
    data class HarMeldeperioderMedKunHelg(val kjedeIder: List<MeldeperiodeKjedeId>) : KunneIkkeOppdatereHelgForMeldekort
}
