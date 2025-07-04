package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Ulid

sealed interface BeregningKilde {
    val id: Ulid

    /** @param id Id for meldekort-behandlingen som utløste denne beregningen. Denne kan være ulik [MeldeperiodeBeregning.meldekortId] for beregninger som er et resultat av en korrigering som påvirket en påfølgende meldeperiode.
     * */
    data class Meldekort(override val id: MeldekortId) : BeregningKilde

    /** @param id Id for behandlingen/revurderingen som utløste denne beregningen.
     * */
    data class Behandling(override val id: BehandlingId) : BeregningKilde
}
